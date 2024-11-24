#include <iostream>
#include <ldap.h>
#include <mysql_driver.h>
#include <mysql_connection.h>
#include <cppconn/statement.h>
#include <cppconn/prepared_statement.h>
#include <cstdlib>
#include <cstring>

using namespace std;
using namespace sql;

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

    const string db_url = "tcp://127.0.0.1:3306";
    const string db_user = "ram";
    const string db_pass = "Dudububu@27";
    const string db_name = "act_dir";

    try {
        mysql::MySQL_Driver* driver = mysql::get_mysql_driver_instance();
        unique_ptr<Connection> con(driver->connect(db_url, db_user, db_pass));
        con->setSchema(db_name);

        unique_ptr<PreparedStatement> pstmt_insert(con->prepareStatement("INSERT INTO users (first_name, last_name, phone_number) VALUES (?, ?, ?)"));
        unique_ptr<PreparedStatement> pstmt_select(con->prepareStatement("SELECT COUNT(*) FROM users WHERE first_name = ? AND last_name = ? AND phone_number = ?"));

        for (entry = ldap_first_entry(ld, result); entry != NULL; entry = ldap_next_entry(ld, entry)) {
            char* dn = ldap_get_dn(ld, entry); 
            cout << "DN: " << (dn ? dn : "Unknown") << endl;
            ldap_memfree(dn);

            string givenName, sn, telephoneNumber;
            for (attribute = ldap_first_attribute(ld, entry, &ber); attribute != NULL; attribute = ldap_next_attribute(ld, entry, ber)) {
                if ((values = ldap_get_values_len(ld, entry, attribute)) != NULL) {
                    if (strcmp(attribute, "givenName") == 0) {
                        givenName = values[0]->bv_val;
                    } else if (strcmp(attribute, "sn") == 0) {
                        sn = values[0]->bv_val;
                    } else if (strcmp(attribute, "telephoneNumber") == 0) {
                        telephoneNumber = values[0]->bv_val;
                    }
                    ldap_value_free_len(values);
                }
                ldap_memfree(attribute);
            }
            pstmt_select->setString(1, givenName);
            pstmt_select->setString(2, sn);
            pstmt_select->setString(3, telephoneNumber);
            unique_ptr<ResultSet> res(pstmt_select->executeQuery());
            res->next();
            if (res->getInt(1) == 0) {
                if (!givenName.empty() && !sn.empty() && !telephoneNumber.empty()) {
                    pstmt_insert->setString(1, givenName);
                    pstmt_insert->setString(2, sn);
                    pstmt_insert->setString(3, telephoneNumber);
                    pstmt_insert->execute();
                    cout << "Inserted: " << givenName << ", " << sn << ", " << telephoneNumber << endl;
                }
            } else {
                cout << "Record already exists: " << givenName << ", " << sn << ", " << telephoneNumber << endl;
            }
            cout << "------------------------------------" << endl;
        }

        pstmt_insert->close();
        pstmt_select->close();
        con->close();
    } catch (SQLException &e) {
        cerr << "MySQL error: " << e.what() << endl;
        ldap_msgfree(result);
        ldap_unbind_ext_s(ld, NULL, NULL);
        return EXIT_FAILURE;
    }
    ber_free(ber, 0);
    ldap_msgfree(result);
    ldap_unbind_ext_s(ld, NULL, NULL);

    cout << "LDAP operations completed." << endl;
    return EXIT_SUCCESS;
}
