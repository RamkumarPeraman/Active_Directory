#include <iostream>
#include <string>
#include <ldap.h>
#include <curl/curl.h>
#include <algorithm>

using namespace std;

const char* ldap_server = "ldap://192.168.87.35";
const char* bind_dn = "CN=Administrator,CN=Users,DC=zoho,DC=com";
const char* password = "Ram@123";
const char* base_dn = "CN=Deleted Objects,DC=zoho,DC=com";
const char* servlet_url = "http://localhost:8080/backend_war_exploded/DeleteDataServlet";

// Function to trim leading and trailing spaces from a string
string trim(const string &str) {
    size_t start = str.find_first_not_of(" \t\r\n");
    size_t end = str.find_last_not_of(" \t\r\n");
    return (start == string::npos || end == string::npos) ? "" : str.substr(start, end - start + 1);
}

void sendDataToServlet(const std::string& servletUrl, const std::string& postData) {
    CURL *curl;
    CURLcode res;
    curl_global_init(CURL_GLOBAL_DEFAULT);
    curl = curl_easy_init();
    if(curl) {
        curl_easy_setopt(curl, CURLOPT_URL, servletUrl.c_str());
        curl_easy_setopt(curl, CURLOPT_POSTFIELDS, postData.c_str());
        res = curl_easy_perform(curl);

        if(res != CURLE_OK) {
            std::cerr << "CURL failed: " << curl_easy_strerror(res) << std::endl;
        }

        curl_easy_cleanup(curl);
    }
    curl_global_cleanup();
}

void fetchDeletedObjects(LDAP* ld, const char* base_dn) {
    // Corrected the control structure initialization
        LDAPControl show_deleted_control = {(char*)"1.2.840.113556.1.4.417", {0, NULL}, 1};

    LDAPControl *server_controls[] = {&show_deleted_control, nullptr};

    LDAPMessage* result = nullptr;

    // Perform the search to fetch deleted objects
    int rc = ldap_search_ext_s(
        ld,
        base_dn,
        LDAP_SCOPE_SUBTREE,
        "(isDeleted=TRUE)",  // Filter to fetch only deleted objects
        nullptr, // Fetch all attributes
        0,
        server_controls,
        nullptr,
        nullptr,
        LDAP_NO_LIMIT,
        &result
    );

    if (rc != LDAP_SUCCESS) {
        std::cerr << "ldap_search_ext_s: " << ldap_err2string(rc) << std::endl;
        ldap_unbind_ext_s(ld, nullptr, nullptr);
        return;
    }

    for (LDAPMessage* entry = ldap_first_entry(ld, result); entry != nullptr; entry = ldap_next_entry(ld, entry)) {
        char* dn = ldap_get_dn(ld, entry);
        std::string objectType, objectName, objectDescription;

        // Fetch "objectClass" (could be any relevant attribute for object type)
        struct berval** values = ldap_get_values_len(ld, entry, "objectClass");
        if (values != nullptr) {
            for (int i = 0; values[i] != nullptr; ++i) {
                std::string objectClass = values[i]->bv_val;
                if (objectClass == "group" || objectClass == "user" || objectClass == "computer" || objectClass == "organizationalUnit") {
                    objectType = objectClass;
                    break;
                }
            }
            ldap_value_free_len(values);
        }

        // Fetch "cn" (common name)
        values = ldap_get_values_len(ld, entry, "cn");
        if (values != nullptr && values[0] != nullptr) {
            objectName = values[0]->bv_val;
            objectName = trim(objectName);  // Trim any leading/trailing spaces

            // Skip entries starting with "DEL:"
            if (objectName.find("DEL:") == 0) {
                continue; // Skip this entry as it's a deleted object
            }

            ldap_value_free_len(values);
        }

        // Fetch "description"
        values = ldap_get_values_len(ld, entry, "description");
        if (values != nullptr) {
            objectDescription = values[0]->bv_val;
            ldap_value_free_len(values);
        }

        // Send data to servlet if not processed yet
        if (!objectType.empty() && !objectName.empty() && !objectDescription.empty()) {
            // Construct the data to send to the servlet
            int len = objectName.size();
            string objName = objectName.substr(0,len-41);
            std::string deleteObjData = "type=" + objectType + "&objectName=" + objName + "&description=" + objectDescription;
            sendDataToServlet(servlet_url, deleteObjData);  // Send data to servlet
            std::cout << "Deleted object data sent to servlet: "
                      << objName << ", " << objectType << ", " << objectDescription << std::endl;
        }

        ldap_memfree(dn);
    }

    ldap_msgfree(result);
}

int main() {
    LDAP *ld;
    int version = LDAP_VERSION3;
    int rc;

    // Initialize LDAP connection
    rc = ldap_initialize(&ld, ldap_server);
    if (rc != LDAP_SUCCESS) {
        std::cerr << "ldap_initialize: " << ldap_err2string(rc) << std::endl;
        return 1;
    }

    // Set LDAP version
    ldap_set_option(ld, LDAP_OPT_PROTOCOL_VERSION, &version);

    // Bind to the server using simple bind
    rc = ldap_sasl_bind_s(ld, bind_dn, LDAP_SASL_SIMPLE, ber_bvstr(password), nullptr, nullptr, nullptr);
    if (rc != LDAP_SUCCESS) {
        std::cerr << "ldap_sasl_bind_s: " << ldap_err2string(rc) << std::endl;
        ldap_unbind_ext_s(ld, nullptr, nullptr);
        return 1;
    }

    // Fetch deleted objects
    fetchDeletedObjects(ld, base_dn);

    // Clean up
    ldap_unbind_ext_s(ld, nullptr, nullptr);
    return 0;
}
