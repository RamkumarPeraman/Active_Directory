#include <iostream>
#include <string>
#include <ldap.h>
#include <ctime>
#include <unistd.h>
#include <curl/curl.h>
#include <unordered_set>
#include <vector>
#include <cstdlib>
#include <cstring>

using namespace std;

// LDAP connection
const char* ldap_server = "ldap://192.168.87.35";
const char* username = "CN=Administrator,CN=Users,DC=zoho,DC=com";
const char* password = "Ram@123";
const char* comp_base_dn = "CN=Computers,DC=zoho,DC=com";
const char* user_base_dn = "CN=Users,DC=zoho,DC=com";

const char* dlt_base_dn = "CN=Deleted Objects,DC=zoho,DC=com";
LDAPControl show_deleted_control = {(char*)"1.2.840.113556.1.4.417", {0, NULL}, 1};
LDAPControl *server_controls[] = {&show_deleted_control, NULL};
vector<string> ou_names;

// Global variables
time_t lastCheckedTime = 0;
char* attribute; 
BerElement* ber;  
LDAPMessage* result, *entry;  
struct berval** values; 
unordered_set<string> processedEntries;
LDAP* ld;
int rc;
bool initialFetch = true;

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

// LDAP binding
void ldapBind() {
    rc = ldap_initialize(&ld, ldap_server);
    if (rc != LDAP_SUCCESS) {
        cerr << "Failed to initialize LDAP connection: " << ldap_err2string(rc) << endl;
        exit(EXIT_FAILURE);
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
        exit(EXIT_FAILURE);
    }
}

// Get formatted time string for LDAP filter
string getLDAPTimeString(time_t rawtime) {
    struct tm* timeinfo;
    char buffer[20]; // YYYYMMDDHHMMSS.0Z
    timeinfo = gmtime(&rawtime);
    strftime(buffer, sizeof(buffer), "%Y%m%d%H%M%S.0Z", timeinfo);
    return string(buffer);
}

// Check the last modification time
time_t getLastModificationTime(LDAP* ld, LDAPMessage* entry) {
    struct berval** values = ldap_get_values_len(ld, entry, "whenChanged");
    time_t lastModTime = 0;
    if (values != NULL) {
        struct tm tm = {};
        if (strptime(values[0]->bv_val, "%Y%m%d%H%M%S.0Z", &tm) != NULL) {
            lastModTime = mktime(&tm);
        }
        ldap_value_free_len(values);
    }
    return lastModTime;
}

// Fetch user data
void fetchUserData(LDAP* ld, const char* base_dn, const char* filter, time_t lastCheckedTime, unordered_set<string>& processedEntries) {
    string timeFilter;
    string combinedFilter;
    if(!initialFetch){
        timeFilter = "(whenChanged>=" + getLDAPTimeString(lastCheckedTime) + ")";
        combinedFilter = "(&" + string(filter) + timeFilter + ")";
    } 
    else {
        combinedFilter = string(filter);
    }
    const char* user_attributes[] = {"givenName", "sn", "telephoneNumber", "mail", "physicalDeliveryOfficeName", "description", "whenChanged", NULL};
    
    LDAPMessage* result = nullptr;
    rc = ldap_search_ext_s(ld, base_dn, LDAP_SCOPE_SUBTREE, combinedFilter.c_str(), const_cast<char**>(user_attributes), 0, NULL, NULL, NULL, 0, &result);
    if (rc != LDAP_SUCCESS) {
        // cerr << "LDAP user search failed: " << ldap_err2string(rc) << endl;
        // return;
    }

    for (LDAPMessage* entry = ldap_first_entry(ld, result); entry != NULL; entry = ldap_next_entry(ld, entry)) {
        time_t entryLastModTime = getLastModificationTime(ld, entry);
        string dn = ldap_get_dn(ld, entry);

        if (initialFetch || (entryLastModTime > lastCheckedTime && processedEntries.find(dn) == processedEntries.end())) {
            string givenName, sn, telephoneNumber, mail, office, description;

            struct berval** values = ldap_get_values_len(ld, entry, "givenName");
            if (values != NULL) {
                givenName = values[0]->bv_val;
                ldap_value_free_len(values);
            }

            values = ldap_get_values_len(ld, entry, "sn");
            if (values != NULL) {
                sn = values[0]->bv_val;
                ldap_value_free_len(values);
            }

            values = ldap_get_values_len(ld, entry, "telephoneNumber");
            if (values != NULL) {
                telephoneNumber = values[0]->bv_val;
                ldap_value_free_len(values);
            }

            values = ldap_get_values_len(ld, entry, "mail");
            if (values != NULL) {
                mail = values[0]->bv_val;
                ldap_value_free_len(values);
            }

            values = ldap_get_values_len(ld, entry, "physicalDeliveryOfficeName");
            if (values != NULL) {
                office = values[0]->bv_val;
                ldap_value_free_len(values);
            }

            values = ldap_get_values_len(ld, entry, "description");
            if (values != NULL) {
                description = values[0]->bv_val;
                ldap_value_free_len(values);
            }

            if (!givenName.empty() && !sn.empty() && !telephoneNumber.empty() && !mail.empty() && !office.empty() && !description.empty()) {
                string userName = givenName+" "+sn;
                string userPostData = "type=User&userName="+userName+"&description="+description;

                // cout << userPostData << endl;
                // string userPostData = "givenName=" + givenName + "&sn=" + sn + "&telephoneNumber=" + telephoneNumber +
                //                       "&mail=" + mail + "&office=" + office + "&description=" + description;
                sendDataToServlet("http://localhost:8080/backend_war_exploded/UserDataServlet", userPostData);
                cout << "User data sent to UserDataServlet: " << givenName << ", " << sn << ", " << telephoneNumber << ", " << mail << ", " << office << ", " << description << endl;
            }

            processedEntries.insert(dn);
        }
    }

    ldap_msgfree(result);
}

// Fetch group data
void fetchGroupData(LDAP* ld, const char* base_dn, time_t lastCheckedTime, unordered_set<string>& processedEntries) {
    string timeFilter;
    string combinedFilter;
    if(!initialFetch){
        timeFilter = "(whenChanged>=" + getLDAPTimeString(lastCheckedTime) + ")";
        combinedFilter = "(&(objectClass=group)" + timeFilter + ")";
    } else {
        combinedFilter = "(objectClass=group)";
    }
    const char* group_attributes[] = {"cn", "description", "whenChanged", NULL};

    LDAPMessage* result = nullptr;
    rc = ldap_search_ext_s(ld, base_dn, LDAP_SCOPE_SUBTREE, combinedFilter.c_str(), const_cast<char**>(group_attributes), 0, NULL, NULL, NULL, 0, &result);
    if (rc != LDAP_SUCCESS) {
        // cerr << "LDAP group search failed: " << ldap_err2string(rc) << endl;
        // return;
    }

    for (LDAPMessage* entry = ldap_first_entry(ld, result); entry != NULL; entry = ldap_next_entry(ld, entry)) {
        string dn = ldap_get_dn(ld, entry);
        string groupName, groupDescription;

        struct berval** values = ldap_get_values_len(ld, entry, "cn");
        if (values != NULL) {
            groupName = values[0]->bv_val;
            ldap_value_free_len(values);
        }

        values = ldap_get_values_len(ld, entry, "description");
        if (values != NULL) {
            groupDescription = values[0]->bv_val;
            ldap_value_free_len(values);
        }

        time_t groupLastModTime = getLastModificationTime(ld, entry);
        if (initialFetch || (groupLastModTime > lastCheckedTime && processedEntries.find(dn) == processedEntries.end())) {
            if (!groupName.empty() && !groupDescription.empty()) {
                string groupPostData = "type=Group&groupName=" + groupName + "&description=" + groupDescription;
                sendDataToServlet("http://localhost:8080/backend_war_exploded/GroupDataServlet", groupPostData);
                cout << "Group data sent to GroupDataServlet: " << groupName << ", " << groupDescription << endl;
                processedEntries.insert(dn);
            }
        }
    }

    ldap_msgfree(result);
}

// Fetch computer data
void fetchComputerData(LDAP* ld, const char* base_dn, time_t lastCheckedTime, unordered_set<string>& processedEntries) {
    string timeFilter;
    string combinedFilter;
    if(!initialFetch){
        timeFilter = "(whenChanged>=" + getLDAPTimeString(lastCheckedTime) + ")";
        combinedFilter = "(&(objectClass=computer)" + timeFilter + ")";
    } else {
        combinedFilter = "(objectClass=computer)";
    }
    const char* computer_attributes[] = {"cn", "description", "whenChanged", NULL};

    LDAPMessage* result = nullptr;
    rc = ldap_search_ext_s(ld, base_dn, LDAP_SCOPE_SUBTREE, combinedFilter.c_str(), const_cast<char**>(computer_attributes), 0, NULL, NULL, NULL, 0, &result);
    if (rc != LDAP_SUCCESS) {
        // cerr << "LDAP computer search failed: " << ldap_err2string(rc) << endl;
        // return;
    }

    for (LDAPMessage* entry = ldap_first_entry(ld, result); entry != NULL; entry = ldap_next_entry(ld, entry)) {
        string dn = ldap_get_dn(ld, entry);
        string computerName, computerDescription;

        struct berval** values = ldap_get_values_len(ld, entry, "cn");
        if (values != NULL) {
            computerName = values[0]->bv_val;
            ldap_value_free_len(values);
        }

        values = ldap_get_values_len(ld, entry, "description");
        if (values != NULL) {
            computerDescription = values[0]->bv_val;
            ldap_value_free_len(values);
        }

        time_t computerLastModTime = getLastModificationTime(ld, entry);
        if (initialFetch || (computerLastModTime > lastCheckedTime && processedEntries.find(dn) == processedEntries.end())) {
            if (!computerName.empty() && !computerDescription.empty()) {
                string type = "Computer";
                string computerPostData = "type=Computer&computerName=" + computerName + "&description=" + computerDescription;
                sendDataToServlet("http://localhost:8080/backend_war_exploded/ComputerDataServlet", computerPostData);
                cout << "Computer data sent to ComputerDataServlet: " << computerName << ", " << computerDescription << endl;
                processedEntries.insert(dn);
            }
        }
    }

    ldap_msgfree(result);
}

// Fetch organizational unit data
void fetchOUData(LDAP* ld, const char* base_dn, time_t lastCheckedTime, unordered_set<string>& processedEntries) {
    string timeFilter;
    string combinedFilter;
    if(!initialFetch){
        timeFilter = "(whenChanged>=" + getLDAPTimeString(lastCheckedTime) + ")";
        combinedFilter = "(&(objectClass=organizationalUnit)" + timeFilter + ")";
    } else {
        combinedFilter = "(objectClass=organizationalUnit)";
    }
    const char* ou_attributes[] = {"ou", "description", "whenChanged", NULL};

    LDAPMessage* result = nullptr;
    rc = ldap_search_ext_s(ld, base_dn, LDAP_SCOPE_SUBTREE, combinedFilter.c_str(), const_cast<char**>(ou_attributes), 0, NULL, NULL, NULL, 0, &result);
    if (rc != LDAP_SUCCESS) {
        // cerr << "LDAP organizational unit search failed: " << ldap_err2string(rc) << endl;
        // return;
    }

    for (LDAPMessage* entry = ldap_first_entry(ld, result); entry != NULL; entry = ldap_next_entry(ld, entry)) {
        string dn = ldap_get_dn(ld, entry);
        string ouName, ouDescription;

        struct berval** values = ldap_get_values_len(ld, entry, "ou");
        if (values != NULL) {
            ouName = values[0]->bv_val;
            ldap_value_free_len(values);
        }

        values = ldap_get_values_len(ld, entry, "description");
        if (values != NULL) {
            ouDescription = values[0]->bv_val;
            ldap_value_free_len(values);
        }

        time_t ouLastModTime = getLastModificationTime(ld, entry);
        if (initialFetch || (ouLastModTime > lastCheckedTime && processedEntries.find(dn) == processedEntries.end())) {
            if (!ouName.empty() && !ouDescription.empty()) {
                string type = "OrganizationUnit";
                string ouPostData = "type="+type+"&ouName=" + ouName + "&description=" + ouDescription;
                sendDataToServlet("http://localhost:8080/backend_war_exploded/OUDataServlet", ouPostData);
                cout << "Organizational Unit data sent to OUDataServlet: " << ouName << ", " << ouDescription << endl;
                processedEntries.insert(dn);
            }
        }
    }

    ldap_msgfree(result);
}

void fetchDeletedObjects(LDAP* ld, const char* base_dn, time_t lastCheckedTime, unordered_set<string>& processedEntries) {
    string timeFilter;
    string combinedFilter;
    if (!initialFetch) {
        timeFilter = "(whenChanged>=" + getLDAPTimeString(lastCheckedTime) + ")";
        combinedFilter = "(&(isDeleted=TRUE)" + timeFilter + ")";
    } else {
        combinedFilter = "(isDeleted=TRUE)";
    }

    LDAPControl show_deleted_control = {(char*)"1.2.840.113556.1.4.417", {0, NULL}, 1};
    LDAPControl* server_controls[] = {&show_deleted_control, nullptr};
    LDAPMessage* result = nullptr;
    int rc = ldap_search_ext_s(ld, base_dn, LDAP_SCOPE_SUBTREE, combinedFilter.c_str(), nullptr, 0, server_controls, nullptr, nullptr, LDAP_NO_LIMIT, &result);
    if (rc != LDAP_SUCCESS) {
        cerr << "ldap_search_ext_s: " << ldap_err2string(rc) << endl;
        ldap_unbind_ext_s(ld, nullptr, nullptr);
        return;
    }

    for (LDAPMessage* entry = ldap_first_entry(ld, result); entry != nullptr; entry = ldap_next_entry(ld, entry)) {
        char* dn = ldap_get_dn(ld, entry);
        string objectType, objectName, objectDescription;

        struct berval** values = ldap_get_values_len(ld, entry, "objectClass");
        if (values != nullptr) {
            for (int i = 0; values[i] != nullptr; ++i) {
                string objectClass = values[i]->bv_val;
                if (objectClass == "group" || objectClass == "user" || objectClass == "computer" || objectClass == "organizationalUnit") {
                    objectType = objectClass;
                    break;
                }
            }
            ldap_value_free_len(values);
        }

        values = ldap_get_values_len(ld, entry, "cn");
        if (values != nullptr && values[0] != nullptr) {
            objectName = values[0]->bv_val;
            ldap_value_free_len(values);
        }

        values = ldap_get_values_len(ld, entry, "description");
        if (values != nullptr) {
            objectDescription = values[0]->bv_val;
            ldap_value_free_len(values);
        }

        if (processedEntries.find(dn) == processedEntries.end()) {
            if (!objectType.empty() && !objectName.empty() && !objectDescription.empty()) {
                int len = objectName.size();
                string objName = objectName.substr(0, len - 41);
                string deleteObjData = "type=" + objectType + "&objectName=" + objName + "&description=" + objectDescription;
                sendDataToServlet("http://localhost:8080/backend_war_exploded/DeletedObjDataServlet", deleteObjData);
                cout << "Deleted object data sent to servlet: " << objName << ", " << objectType << ", " << objectDescription << endl;
                processedEntries.insert(dn);
            }
        }
        ldap_memfree(dn);
    }

    ldap_msgfree(result);
}

//fetch the data from Users
void fetchFromUsers(LDAP* ld,const char* base_dn){
        fetchUserData(ld, base_dn, "(objectClass=user)", lastCheckedTime, processedEntries); 
        fetchGroupData(ld, base_dn, lastCheckedTime, processedEntries);
        fetchComputerData(ld, base_dn, lastCheckedTime, processedEntries);

}

// function to fetc the data from the computer
void fetchFromComputers (LDAP* ld,const char* base_dn){
        fetchComputerData(ld, base_dn, lastCheckedTime, processedEntries);
        fetchUserData(ld, base_dn, "(objectClass=user)", lastCheckedTime, processedEntries); 
        fetchGroupData(ld, base_dn, lastCheckedTime, processedEntries);
}

// function to fetch data from OU's
void fetchFromOU(LDAP* ld,const char* base_dn){
        fetchOUData(ld,base_dn,lastCheckedTime, processedEntries);
        fetchUserData(ld, base_dn, "(objectClass=user)", lastCheckedTime, processedEntries); 
        fetchComputerData(ld, base_dn, lastCheckedTime, processedEntries);
        fetchGroupData(ld, base_dn, lastCheckedTime, processedEntries); 
}

void fetchOu(){
    const char* ou_filter = "(objectClass=organizationalUnit)";
    const char* ou_attributes[] = {"ou", NULL};
    const char* base_dn = "DC=zoho,DC=com";

    rc = ldap_search_ext_s(ld, base_dn, LDAP_SCOPE_SUBTREE, ou_filter, const_cast<char**>(ou_attributes), 0, NULL, NULL, NULL, 0, &result);
    if (rc != LDAP_SUCCESS) {
        cerr << "LDAP OU search failed: " << ldap_err2string(rc) << endl;
        ldap_unbind_ext_s(ld, NULL, NULL);
        return ;
    }
    cout << "Searching for OU objects..." << endl;
    for (entry = ldap_first_entry(ld, result); entry != NULL; entry = ldap_next_entry(ld, entry)) {
        string ouName;
        for (attribute = ldap_first_attribute(ld, entry, &ber); attribute != NULL; attribute = ldap_next_attribute(ld, entry, ber)) {
            if ((values = ldap_get_values_len(ld, entry, attribute)) != NULL) {
                if (strcmp(attribute, "ou") == 0) {
                    ouName = values[0]->bv_val;
                }
                ldap_value_free_len(values);
            }
            ldap_memfree(attribute);
        }
        if (!ouName.empty()) {
            ou_names.push_back("OU="+ouName+",DC=zoho,DC=com"); // storing the OU's in the array
        }
        if (ber != nullptr) {
            ber_free(ber, 0);
        }
    }
    ldap_msgfree(result);
}

int main() {
    ldapBind(); 
    fetchOu();
    while (true) {
        fetchFromUsers(ld,user_base_dn); // fetch the data from the users
        fetchDeletedObjects(ld,dlt_base_dn,lastCheckedTime,processedEntries);
        fetchFromComputers(ld,comp_base_dn); // fetch the data from computers
        for(string ou_base_dn : ou_names){ // fetch the data from each OU's
            fetchFromOU(ld,ou_base_dn.c_str());
        }
        cout << "hpp";
        lastCheckedTime = time(nullptr);
        initialFetch = false;
        

        sleep(60);
    }
    ldap_unbind_ext_s(ld, nullptr, nullptr);

    return 0;
}
