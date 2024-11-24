#include <iostream>
#include <ldap.h>
#include <mysql/mysql.h>
#include <cstdlib>
#include <cstring>

using namespace std;

int main() {
    LDAP* ld;                              
    LDAPMessage* result, *entry;           
    BerElement* ber;                       
    char* attribute;                       
    struct berval** values;                
    int rc;                                

    const char* ldap_server = "ldap://192.168.129.129"; 
    const char* username = "CN=Administrator,CN=Users,DC=zoho,DC=com";  
    const char* password = "Ram@123"; 
    const char* base_dn = "dc=zoho,dc=com";   
    const char* filter = "(objectClass=user)";  
    const char* attributes[] = {"givenName", "sn", "telephoneNumber", NULL}; 

    rc = ldap_initialize(&ld, ldap_server);
    if (rc != LDAP_SUCCESS) {
        cerr << "Failed to initialize LDAP connection: " << ldap_err2string(rc) << endl;
        return EXIT_FAILURE;
    }
    cout << "LDAP connection initialized." << endl;

    int ldap_version = LDAP_VERSION3;
    ldap_set_option(ld, LDAP_OPT_PROTOCOL_VERSION, &ldap_version);

    struct berval cred;
    cred.bv_val = const_cast<char*>(password);
    cred.bv_len = strlen(password);

    rc = ldap_sasl_bind_s(ld, username, LDAP_SASL_SIMPLE, &cred, NULL, NULL, NULL);
    if (rc != LDAP_SUCCESS) {
        cerr << "LDAP bind failed: " << ldap_err2string(rc) << endl;
        ldap_unbind_ext_s(ld, NULL, NULL);
        return EXIT_FAILURE;
    }
    cout << "LDAP bind successful." << endl;

    rc = ldap_search_ext_s(ld, base_dn, LDAP_SCOPE_SUBTREE, filter, const_cast<char**>(attributes), 0, NULL, NULL, NULL, 0, &result);
    if (rc != LDAP_SUCCESS) {
        cerr << "LDAP search failed: " << ldap_err2string(rc) << endl;
        ldap_unbind_ext_s(ld, NULL, NULL);
        return EXIT_FAILURE;
    }
    cout << "LDAP search successful. Processing results..." << endl;

    const char* db_host = "127.0.0.1";
    const char* db_user = "ram";
    const char* db_pass = "Dudububu@27";
    const char* db_name = "act_dir";

    MYSQL* conn;
    conn = mysql_init(NULL);
    if (conn == NULL) {
        cerr << "MySQL initialization failed" << endl;
        ldap_msgfree(result);
        ldap_unbind_ext_s(ld, NULL, NULL);
        return EXIT_FAILURE;
    }
    if (mysql_real_connect(conn, db_host, db_user, db_pass, db_name, 0, NULL, 0) == NULL) {
        cerr << "MySQL connection failed: " << mysql_error(conn) << endl;
        mysql_close(conn);
        ldap_msgfree(result);
        ldap_unbind_ext_s(ld, NULL, NULL);
        return EXIT_FAILURE;
    }
    for (entry = ldap_first_entry(ld, result); entry != NULL; entry = ldap_next_entry(ld, entry)) {
        char* dn = ldap_get_dn(ld, entry); 
        cout << "DN: " << (dn ? dn : "Unknown") << endl;
        ldap_memfree(dn);

        string givenName, sn, telephoneNumber;
        for (attribute = ldap_first_attribute(ld, entry, &ber); attribute != NULL; attribute = ldap_next_attribute(ld, entry, ber)) {
            if ((values = ldap_get_values_len(ld, entry, attribute)) != NULL) {
                if(strcmp(attribute, "givenName") == 0) {
                    givenName = values[0]->bv_val;
                } 
                else if(strcmp(attribute, "sn") == 0) {
                    sn = values[0]->bv_val;
                } 
                else if (strcmp(attribute, "telephoneNumber") == 0) {
                    telephoneNumber = values[0]->bv_val;
                }
                ldap_value_free_len(values);
            }
            ldap_memfree(attribute);
        }

        if (!givenName.empty() && !sn.empty() && !telephoneNumber.empty()) {
            string select_query = "SELECT COUNT(*) FROM users WHERE first_name = '" + givenName + "' AND last_name = '" + sn + "' AND phone_number = '" + telephoneNumber + "'";
            if (mysql_query(conn, select_query.c_str())) {
                cerr << "MySQL query failed: " << mysql_error(conn) << endl;
                mysql_close(conn);
                ldap_msgfree(result);
                ldap_unbind_ext_s(ld, NULL, NULL);
                return EXIT_FAILURE;
            }
            MYSQL_RES* res = mysql_store_result(conn);
            if (res == NULL) {
                cerr << "MySQL store result failed: " << mysql_error(conn) << endl;
                mysql_close(conn);
                ldap_msgfree(result);
                ldap_unbind_ext_s(ld, NULL, NULL);
                return EXIT_FAILURE;
            }
            MYSQL_ROW row = mysql_fetch_row(res);
            int count = atoi(row[0]);
            mysql_free_result(res);

            if(count == 0) {
                string insert_query = "INSERT INTO users (first_name, last_name, phone_number) VALUES ('" + givenName + "', '" + sn + "', '" + telephoneNumber + "')";
                if (mysql_query(conn, insert_query.c_str())) {
                    cerr << "MySQL insert failed: " << mysql_error(conn) << endl;
                    mysql_close(conn);
                    ldap_msgfree(result);
                    ldap_unbind_ext_s(ld, NULL, NULL);
                    return EXIT_FAILURE;
                }
                cout << "Inserted: " << givenName << ", " << sn << ", " << telephoneNumber << endl;
            } 
            else {
                cout << "Record already exists: " << givenName << ", " << sn << ", " << telephoneNumber << endl;
            }
        }
        cout << "------------------------------------" << endl;
    }

    mysql_close(conn);
    ber_free(ber, 0);
    ldap_msgfree(result);
    ldap_unbind_ext_s(ld, NULL, NULL);

    cout << "LDAP operations completed." << endl;
    return EXIT_SUCCESS;
}
