#include <iostream>
#include <ldap.h>
#include <cstdlib>
#include <cstring>
#include <curl/curl.h>

using namespace std;

// Data send to servlet 

void sendDataToServlet(const string& servletUrl, const string& postData) {
    CURL *curl;
    CURLcode res;
    curl_global_init(CURL_GLOBAL_DEFAULT);
    curl = curl_easy_init();
    if(curl) {
        curl_easy_setopt(curl, CURLOPT_URL, servletUrl.c_str());
        curl_easy_setopt(curl, CURLOPT_POSTFIELDS, postData.c_str());
        res = curl_easy_perform(curl);

        if(res != CURLE_OK) {
            cerr << "CURL failed: " << curl_easy_strerror(res) << endl;
        }

        curl_easy_cleanup(curl);
    }
    curl_global_cleanup();
}


// Fetch User Data
void fetchUserData(LDAP* ld, const char* base_dn, int rc) {
    LDAPMessage* result, *entry;           
    BerElement* ber;                       
    char* attribute;                       
    struct berval** values; 

    const char* user_filter = "(objectClass=user)";
    const char* user_attributes[] = {"givenName", "sn", "telephoneNumber", "mail", "physicalDeliveryOfficeName", "description", NULL};


    rc = ldap_search_ext_s(ld, base_dn, LDAP_SCOPE_SUBTREE, user_filter, const_cast<char**>(user_attributes), 0, NULL, NULL, NULL, 0, &result);
    if (rc != LDAP_SUCCESS) {
        cerr << "LDAP user search failed: " << ldap_err2string(rc) << endl;
        return;
    }
    cout << "Searching for User objects..." << endl;

    for (entry = ldap_first_entry(ld, result); entry != NULL; entry = ldap_next_entry(ld, entry)) {
        string givenName, sn, telephoneNumber, mail, office, description;

        for (attribute = ldap_first_attribute(ld, entry, &ber); attribute != NULL; attribute = ldap_next_attribute(ld, entry, ber)) {
            if ((values = ldap_get_values_len(ld, entry, attribute)) != NULL) {
                if (strcmp(attribute, "givenName") == 0) {
                    givenName = values[0]->bv_val;
                } 
                else if (strcmp(attribute, "sn") == 0) {
                    sn = values[0]->bv_val;
                } 
                else if (strcmp(attribute, "telephoneNumber") == 0) {
                    telephoneNumber = values[0]->bv_val;
                }
                else if (strcmp(attribute, "mail") == 0) {
                    mail = values[0]->bv_val;  
                }
                else if (strcmp(attribute, "physicalDeliveryOfficeName") == 0) {
                    office = values[0]->bv_val;
                }
                else if (strcmp(attribute, "description") == 0) {
                    description = values[0]->bv_val;
                }
                ldap_value_free_len(values);
            }
            ldap_memfree(attribute);
        }

        if (!givenName.empty() && !sn.empty() && !telephoneNumber.empty() && !mail.empty() && !office.empty() && !description.empty()) {
            string userPostData = "givenName=" + givenName + "&sn=" + sn + "&telephoneNumber=" + telephoneNumber +
                                  "&mail=" + mail + "&office=" + office + "&description=" + description;
            sendDataToServlet("http://localhost:8080/backend_war_exploded/UserDataServlet", userPostData);
            cout << "User data sent to UserDataServlet: " << givenName << ", " << sn << ", " << telephoneNumber << ", " << mail << ", " << office << ", " << description << endl;
        }
    }

    ldap_msgfree(result);
}


// fetch organizationalUnit
void fetchOUData(LDAP* ld, const char* base_dn, int rc){
    LDAPMessage* result, *entry;           
    BerElement* ber;                       
    char* attribute;                       
    struct berval** values; 

    const char* ou_filter = "(objectClass=organizationalUnit)";
    const char* ou_attributes[] = {"ou", "description", NULL};

    rc = ldap_search_ext_s(ld, base_dn, LDAP_SCOPE_SUBTREE, ou_filter, const_cast<char**>(ou_attributes), 0, NULL, NULL, NULL, 0, &result);
    if (rc != LDAP_SUCCESS) {
        cerr << "LDAP OU search failed: " << ldap_err2string(rc) << endl;
        ldap_unbind_ext_s(ld, NULL, NULL);
        return;
    }
    cout << "Searching for OU objects..." << endl;
    for (entry = ldap_first_entry(ld, result); entry != NULL; entry = ldap_next_entry(ld, entry)) {
        string ouName, ouDescription;
        for (attribute = ldap_first_attribute(ld, entry, &ber); attribute != NULL; attribute = ldap_next_attribute(ld, entry, ber)) {
            if ((values = ldap_get_values_len(ld, entry, attribute)) != NULL) {
                if (strcmp(attribute, "ou") == 0) {
                    ouName = values[0]->bv_val;
                }
                else if (strcmp(attribute, "description") == 0) {
                    ouDescription = values[0]->bv_val;
                }
                ldap_value_free_len(values);
            }
            ldap_memfree(attribute);
        }
        if (!ouName.empty() && !ouDescription.empty()) {
            string ouPostData = "ouName=" + ouName + "&description=" + ouDescription;
            sendDataToServlet("http://localhost:8080/backend_war_exploded/OUDataServlet", ouPostData);
            cout << "OU data sent to OUDataServlet: " << ouName << ", " << ouDescription << endl;
        }
    }
    ldap_msgfree(result);

}

// Fetch GroupData 
void fetchGroupData(LDAP* ld, const char* base_dn, int rc){
    LDAPMessage* result, *entry;           
    BerElement* ber;                       
    char* attribute;                       
    struct berval** values;   

    const char* group_filter = "(objectClass=group)";  
    const char* group_attributes[] = {"cn", "description", NULL};

    rc = ldap_search_ext_s(ld, base_dn, LDAP_SCOPE_SUBTREE, group_filter, const_cast<char**>(group_attributes), 0, NULL, NULL, NULL, 0, &result);
    if (rc != LDAP_SUCCESS) {
        cerr << "LDAP Group search failed: " << ldap_err2string(rc) << endl;
        ldap_unbind_ext_s(ld, NULL, NULL);
        return;
    }

    cout << "Searching for Group objects..." << endl;
    for (entry = ldap_first_entry(ld, result); entry != NULL; entry = ldap_next_entry(ld, entry)) {
        string groupName, groupDescription;

        for (attribute = ldap_first_attribute(ld, entry, &ber); attribute != NULL; attribute = ldap_next_attribute(ld, entry, ber)) {
            if ((values = ldap_get_values_len(ld, entry, attribute)) != NULL) {
                if (strcmp(attribute, "cn") == 0) {
                    groupName = values[0]->bv_val;
                }
                else if (strcmp(attribute, "description") == 0) {
                    groupDescription = values[0]->bv_val;
                }
                ldap_value_free_len(values);
            }
            ldap_memfree(attribute);
        }

        if (!groupName.empty()&& !groupDescription.empty()) {
            string groupPostData = "groupName=" + groupName + "&description=" + groupDescription;
            sendDataToServlet("http://localhost:8080/backend_war_exploded/GroupDataServlet", groupPostData);
            cout << "Group data sent to GroupDataServlet: " << groupName << ", " << groupDescription << endl;
        }
    }

    ldap_msgfree(result);
}

// fetch ComputerData
void fetchComputerData(LDAP* ld, const char* base_dn, int rc){
    LDAPMessage* result, *entry;           
    BerElement* ber;                       
    char* attribute;                       
    struct berval** values; 

    const char* computer_filter = "(objectClass=computer)";
    const char* computer_attributes[] = {"cn", "physicalDeliveryOfficeName", "description", NULL};

    rc = ldap_search_ext_s(ld, base_dn, LDAP_SCOPE_SUBTREE, computer_filter, const_cast<char**>(computer_attributes), 0, NULL, NULL, NULL, 0, &result);
    if (rc != LDAP_SUCCESS) {
        cerr << "LDAP computer search failed: " << ldap_err2string(rc) << endl;
        ldap_unbind_ext_s(ld, NULL, NULL);
        return;
    }

    for (entry = ldap_first_entry(ld, result); entry != NULL; entry = ldap_next_entry(ld, entry)) {
        string computerName, description;

        for (attribute = ldap_first_attribute(ld, entry, &ber); attribute != NULL; attribute = ldap_next_attribute(ld, entry, ber)) {
            if ((values = ldap_get_values_len(ld, entry, attribute)) != NULL) {
                if (strcmp(attribute, "cn") == 0) {
                    computerName = values[0]->bv_val;
                } else if (strcmp(attribute, "description") == 0) {
                    description = values[0]->bv_val;
                }
                ldap_value_free_len(values);
            }
            ldap_memfree(attribute);
        }

        if (!computerName.empty() && !description.empty()) {
            string computerPostData = "computerName=" + computerName + "&description=" + description;
            sendDataToServlet("http://localhost:8080/backend_war_exploded/ComputerDataServlet", computerPostData);
            cout << "Computer data sent to ComputerDataServlet: " << computerName << ", " << description << endl;
        }
    }

    ldap_msgfree(result);
}

// Fetch all datas 
void fetchHometData(LDAP* ld, const char* base_dn, int rc){

    fetchUserData(ld, base_dn, rc);
    fetchOUData(ld,base_dn,rc);
    fetchGroupData(ld,base_dn,rc);
    fetchComputerData(ld,base_dn,rc);
}


// Fetch all computers 
void fetchOU(LDAP* ld, const char* base_dn, int rc){

    fetchUserData(ld, base_dn, rc);
    fetchOUData(ld,base_dn,rc);
    fetchGroupData(ld,base_dn,rc);
    fetchComputerData(ld,base_dn,rc);

}

// Fetch all Users 
void fetchUsers(LDAP* ld, const char* base_dn, int rc){

    fetchUserData(ld, base_dn, rc);
    fetchOUData(ld,base_dn,rc);
    fetchGroupData(ld,base_dn,rc);
    fetchComputerData(ld,base_dn,rc);

}

// Fetch all Computers 
void fetchComputers(LDAP* ld, const char* base_dn, int rc){
    fetchUserData(ld, base_dn, rc);
    fetchOUData(ld,base_dn,rc);
    fetchGroupData(ld,base_dn,rc);
    fetchComputerData(ld,base_dn,rc);

}

// Main function
int main(){
    const char* ldap_server = "ldap://10.94.74.195"; // win server ip
    const char* username = "CN=Administrator,CN=Users,DC=zoho,DC=com";  
    const char* password = "Ram@123";
    const char* base_dn = "dc=zoho,dc=com"; 
    const char* user_base_dn = "CN=Users,dc=zoho,dc=com";   
    const char* comp_base_dn = "CN=Computers,dc=zoho,dc=com"; 
    const char* ou_base_dn = "OU=zoho_org,dc=zoho,dc=com"; 

    LDAP* ld;
    int rc;  

    rc = ldap_initialize(&ld, ldap_server);
    cout << "Process start" << endl;
    if (rc != LDAP_SUCCESS) {
        cerr << "Failed to initialize LDAP connection: " << ldap_err2string(rc) << endl;
        return EXIT_FAILURE;
    }
      
    int ldap_version = LDAP_VERSION3;
    ldap_set_option(ld, LDAP_OPT_PROTOCOL_VERSION, &ldap_version);

    BerValue cred;
    cred.bv_val = (char*)password;
    cred.bv_len = strlen(password);

    rc = ldap_sasl_bind_s(ld, username, LDAP_SASL_SIMPLE, &cred, NULL, NULL, NULL);
    if (rc != LDAP_SUCCESS) {
        cerr << "LDAP bind failed: " << ldap_err2string(rc) << endl;
        ldap_unbind_ext_s(ld, NULL, NULL);
        return EXIT_FAILURE;
    }
    // fetchHometData(ld,base_dn,rc);
    fetchUsers(ld,user_base_dn,rc);
    fetchComputers(ld, comp_base_dn, rc);
    fetchOU(ld,ou_base_dn,rc);


    ldap_unbind_ext_s(ld, NULL, NULL);
    return EXIT_SUCCESS;
}
