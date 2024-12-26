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
const char* ldap_server = "ldap://192.168.228.35";
const char* username = "CN=Administrator,CN=Users,DC=zoho,DC=com";
const char* password = "Ram@123";
const char* comp_base_dn = "CN=Computers,DC=zoho,DC=com";
const char* user_base_dn = "CN=Users,DC=zoho,DC=com";
const char* dlt_base_dn = "CN=Deleted Objects,DC=zoho,DC=com";
string URL = "http://localhost:8080/backend_war_exploded";

// Global variables
vector<string> ou_names;
time_t lastCheckedTime = 0;
char* attribute; 
BerElement* ber;  
LDAPMessage* result, *entry;  
struct berval** values; 
unordered_set<string> processedEntries;
LDAP* ld;
int rc;
bool initialFetch = true;
bool servletSend = false;
string timeFilter;
string combinedFilter;
string  userData,groupData,computerData,ouData,deletedObjectData;

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
string getLDAPTimeString(time_t rawtime) {
    struct tm* timeinfo;
    char buffer[20]; 
    timeinfo = gmtime(&rawtime);
    strftime(buffer, sizeof(buffer), "%Y%m%d%H%M%S.0Z", timeinfo);
    return string(buffer);
}
time_t getLastModificationTime(LDAP* ld, LDAPMessage* entry) {// Check the last modification time
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
void dataTraverse(const char* base_dn, const char* filter, const char* attributes[], void (*processEntry)(LDAP* ld, LDAPMessage* entry)) {
    LDAPMessage* result = nullptr;
    int rc = ldap_search_ext_s(ld, base_dn, LDAP_SCOPE_SUBTREE, filter, const_cast<char**>(attributes), 0, NULL, NULL, NULL, 0, &result);
    if (rc == LDAP_SUCCESS) {    
        for (LDAPMessage* entry = ldap_first_entry(ld, result); entry != NULL; entry = ldap_next_entry(ld, entry)) {
            time_t entryLastModTime = getLastModificationTime(ld, entry);
            string dn = ldap_get_dn(ld, entry);
            if (initialFetch || (entryLastModTime > lastCheckedTime && processedEntries.find(dn) == processedEntries.end())) {
                processEntry(ld, entry);
                processedEntries.insert(dn);
            }
        }
    }
    ldap_msgfree(result);
}
void dataAddToVal(struct berval** values, string& val){// Data add to values
    if(values != NULL) {
        val = values[0]->bv_val;
        ldap_value_free_len(values);
    }
}
void processUserEntry(LDAP* ld, LDAPMessage* entry){// user entery
    string givenName, sn, description;
    struct berval** values = ldap_get_values_len(ld, entry, "givenName");
    dataAddToVal(values,givenName);
    values = ldap_get_values_len(ld, entry, "sn");
    dataAddToVal(values,sn);
    values = ldap_get_values_len(ld, entry, "description");
    dataAddToVal(values,description);

    if (!givenName.empty() && !sn.empty() && !description.empty()) {
        string userName = givenName + " " + sn;
        string userPostData = "{\"userName\":\"" + userName + "\", \"description\":\"" + description +"\"},";
        // sendDataToServlet(URL+"/UserDataServlet", userPostData);
        userData += userPostData;
        servletSend = false;                
        // cout << "User data sent to UserDataServlet: " << givenName << ", " << sn << ", " << description << endl;
    }
}
void fetchUserData(const char* base_dn, const char* filter){// Fetch user data
    if (!initialFetch) {
        timeFilter = "(whenChanged>=" + getLDAPTimeString(lastCheckedTime) + ")";
        combinedFilter = "(&" + string(filter) + timeFilter + ")";
    } else {
        combinedFilter = string(filter);
    }
    const char* user_attributes[] = {"givenName", "sn", "description", "whenChanged", NULL};
    userData += "[";
    dataTraverse(base_dn, combinedFilter.c_str(), user_attributes, processUserEntry);
    if(!userData.empty() && !servletSend){
        if (userData.back() == ',') {
            userData.pop_back();
        }
        userData += "]";
        string finalData = "{\"type\": \"User\", \"Users\": " + userData + "}";
        sendDataToServlet(URL+"/UserDataServlet", finalData);
        cout << "-------------------------------------------------------"<< endl;
        cout << "All User data sent to UserDataServlet: " << endl << finalData << endl;
        servletSend = true;
        userData ="";
    }
}

void processGroupEntry(LDAP* ld, LDAPMessage* entry){
    string groupName, groupDescription;
    struct berval** values = ldap_get_values_len(ld, entry, "cn");
    dataAddToVal(values, groupName);
    values = ldap_get_values_len(ld, entry, "description");
    dataAddToVal(values, groupDescription);

    if (!groupName.empty() && !groupDescription.empty()) {
        string groupPostData = "{\"groupName\":\"" + groupName + "\", \"description\":\"" + groupDescription + "\"},";
        groupData += groupPostData;
        servletSend = false;
    }
}
void fetchGroupData(const char* base_dn){
    string combinedFilter;
    if (!initialFetch) {
        string timeFilter = "(whenChanged>=" + getLDAPTimeString(lastCheckedTime) + ")";
        combinedFilter = "(&(objectClass=group)" + timeFilter + ")";
    } else {
        combinedFilter = "(objectClass=group)";
    }
    const char* group_attributes[] = {"cn", "description", "whenChanged", NULL};
    groupData += "[";
    dataTraverse(base_dn, combinedFilter.c_str(), group_attributes, processGroupEntry);
    if (!groupData.empty() && !servletSend) {
        if (groupData.back() == ',') {
            groupData.pop_back();
        }
        groupData += "]";
        string finalData = "{\"type\": \"group\", \"groups\": " + groupData + "}";
        sendDataToServlet(URL + "/GroupDataServlet", finalData);
        cout << "-------------------------------------------------------" << endl;
        cout << "All group data sent to GroupDataServlet: " << endl << finalData << endl;
        servletSend = true;
        groupData = ""; 
    }
}
void processComputerEntry(LDAP* ld, LDAPMessage* entry) { // Fetch computer data
    string computerName, computerDescription;
    struct berval** values = ldap_get_values_len(ld, entry, "cn");
    dataAddToVal(values, computerName);
    values = ldap_get_values_len(ld, entry, "description");
    dataAddToVal(values, computerDescription);
    time_t computerLastModTime = getLastModificationTime(ld, entry);
    string dn = ldap_get_dn(ld, entry);
    if (!computerName.empty() && !computerDescription.empty()) {
        string computerPostData = "{\"computerName\":\"" + computerName + "\", \"description\":\"" + computerDescription + "\"},";
        // string computerPostData = "&computerName=" + computerName + "&description=" + computerDescription;
        // sendDataToServlet(URL + "/ComputerDataServlet", computerPostData);
        computerData += computerPostData;
        servletSend = false;
        // cout << "Computer data sent to ComputerDataServlet: " << computerName << ", " << computerDescription << endl;
    }
}
void fetchComputerData(const char* base_dn) {
    if (!initialFetch) {
        timeFilter = "(whenChanged>=" + getLDAPTimeString(lastCheckedTime) + ")";
        combinedFilter = "(&(objectClass=computer)" + timeFilter + ")";
    } else {
        combinedFilter = "(objectClass=computer)";
    }
    const char* computer_attributes[] = {"cn", "description", "whenChanged", NULL};
    computerData += "[";
    dataTraverse(base_dn, combinedFilter.c_str(), computer_attributes, processComputerEntry);
    if(!computerData.empty() && !servletSend){
        if (computerData.back() == ',') {
            computerData.pop_back();
        }
        computerData += "]";
        string finalData = "{\"type\": \"computer\", \"computers\": " + computerData + "}";
        sendDataToServlet(URL+"/ComputerDataServlet", finalData);
        cout << "-------------------------------------------------------"<< endl;
        cout << "Computer data sent to ComputerDataServlet: "<< endl << "-------------------------------------------------------" << endl<<finalData << endl;
        servletSend = true;
        computerData = "";

    }
}
void processOUEntry(LDAP* ld, LDAPMessage* entry) {
    string ouName, ouDescription;
    struct berval** values = ldap_get_values_len(ld, entry, "ou");
    dataAddToVal(values, ouName);
    values = ldap_get_values_len(ld, entry, "description");
    dataAddToVal(values, ouDescription);
    time_t ouLastModTime = getLastModificationTime(ld, entry);
    string dn = ldap_get_dn(ld, entry);
    if (!ouName.empty() && !ouDescription.empty()) {
        string type = "OrganizationUnit";
        string ouPostData = "type=" + type + "&ouName=" + ouName + "&description=" + ouDescription;
        sendDataToServlet(URL + "/OUDataServlet", ouPostData);
        ouData = ouPostData +"&";
        // cout << "Organizational Unit data sent to OUDataServlet: " << ouName << ", " << ouDescription << endl;
    }

}
void fetchOUData(const char* base_dn) {// Fetch organizational unit data
    if (!initialFetch) {
        timeFilter = "(whenChanged>=" + getLDAPTimeString(lastCheckedTime) + ")";
        combinedFilter = "(&(objectClass=organizationalUnit)" + timeFilter + ")";
    } else {
        combinedFilter = "(objectClass=organizationalUnit)";
    }
    const char* ou_attributes[] = {"ou", "description", "whenChanged", NULL};
    dataTraverse(base_dn,combinedFilter.c_str(), ou_attributes, processOUEntry);
    if(!ouData.empty() && !servletSend){
        ouData.pop_back();
        sendDataToServlet(URL+"/ComputerDataServlet", ouData);
        cout << "Computer data sent to ComputerDataServlet: " << ouData << endl;
        servletSend = true;
        ouData = "";

    }
}
void fetchDeletedObjects(const char* base_dn) {
    if (!initialFetch) {
        timeFilter = "(whenChanged>=" + getLDAPTimeString(lastCheckedTime) + ")";
        combinedFilter = "(&(isDeleted=TRUE)" + timeFilter + ")";
    } else {
        combinedFilter = "(isDeleted=TRUE)";
    }
    
    LDAPControl deleted_control = {(char*)"1.2.840.113556.1.4.417", {0, NULL}, 1};
    LDAPControl* server_controls[] = {&deleted_control, nullptr};
    LDAPMessage* result = nullptr;
    int rc = ldap_search_ext_s(ld, base_dn, LDAP_SCOPE_SUBTREE, combinedFilter.c_str(), nullptr, 0, server_controls, nullptr, nullptr, LDAP_NO_LIMIT, &result);
    
    if (rc != LDAP_SUCCESS) {
        // cerr << "ldap_search_ext_s: " << ldap_err2string(rc) << endl;
        // ldap_unbind_ext_s(ld, nullptr, nullptr);
        // return;
    }
    string deletedObjectData = "{\"deletedObjects\":[";

    for (LDAPMessage* entry = ldap_first_entry(ld, result); entry != nullptr; entry = ldap_next_entry(ld, entry)) {
        char* dn = ldap_get_dn(ld, entry);
        string objectType, objectName, objectDescription;

        struct berval** values = ldap_get_values_len(ld, entry, "objectClass");
        if (values != nullptr) {
            for (int i = 0; values[i] != nullptr; ++i) {
                string objectClass = values[i]->bv_val;
                if (objectClass == "computer") {
                    objectType = "computer";
                    break;
                } else if (objectClass == "group") {
                    objectType = "group";
                    break;
                } else if (objectClass == "user") {
                    objectType = "user";
                    break;
                } else if (objectClass == "organizationalUnit") {
                    objectType = "organizationalUnit";
                    break;
                }
            }
            ldap_value_free_len(values);
        }
        values = ldap_get_values_len(ld, entry, "cn");
        dataAddToVal(values, objectName);
        values = ldap_get_values_len(ld, entry, "description");
        dataAddToVal(values, objectDescription);

        time_t dltLastModTime = getLastModificationTime(ld, entry);
        if (initialFetch || (dltLastModTime > lastCheckedTime && processedEntries.find(dn) == processedEntries.end())) {
            if (!objectType.empty() && !objectName.empty() && !objectDescription.empty()) {


                int len = objectName.size();
                string objName = objectName.substr(0, len - 41); 
                
                
                deletedObjectData += "{\"type\":\"" + objectType + "\",";
                if (objectType == "user") {
                    deletedObjectData += "\"userName\":\"" + objName + "\",";
                } else if (objectType == "computer") {
                    deletedObjectData += "\"computerName\":\"" + objName + "\",";
                } else if (objectType == "group") {
                    deletedObjectData += "\"groupName\":\"" + objName + "\",";
                }
                deletedObjectData += "\"description\":\"" + objectDescription + "\"},";
                
                processedEntries.insert(dn);
            }
        }

        ldap_memfree(dn);
    }

    if (!deletedObjectData.empty()) {
        deletedObjectData.pop_back();
        deletedObjectData += "]}";
        sendDataToServlet(URL + "/DeletedObjDataServlet", deletedObjectData);
        // cout << "Deleted object data sent to servlet: " << deletedObjectData << endl;
    }

    ldap_msgfree(result);
}


void fetchFromUsers(const char* base_dn){//fetch the data from Users
        fetchUserData(base_dn, "(objectClass=user)"); 
        // fetchGroupData(base_dn);
        // fetchComputerData(base_dn);

}
void fetchFromComputers (const char* base_dn){// function to fetc the data from the computer
        fetchComputerData(base_dn);
        fetchUserData(base_dn, "(objectClass=user)"); 
        fetchGroupData(base_dn);
}
void fetchFromOU(const char* base_dn){// function to fetch data from OU's
        fetchOUData(base_dn);
        fetchUserData(base_dn, "(objectClass=user)"); 
        fetchComputerData(base_dn);
        fetchGroupData(base_dn);
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

int main(){
    ldapBind(); 
    // fetchOu();
    while(true){
        // fetchDeletedObjects(dlt_base_dn);
        fetchFromUsers(user_base_dn);
        // fetchFromComputers(comp_base_dn);
        // for(string ou_base_dn : ou_names){
        //     fetchFromOU(ou_base_dn.c_str());
        // }
        lastCheckedTime = time(nullptr);
        initialFetch = false;        
        sleep(60);
    }
    ldap_unbind_ext_s(ld, nullptr, nullptr);
    return 0;
}