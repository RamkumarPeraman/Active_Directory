    while (true) {
        int option;
        cout << "Select an option:" << endl;
        cout << "1. Fetch User Data" << endl;
        cout << "2. Fetch OU Data" << endl;
        cout << "3. Fetch Group Data" << endl;
        cout << "4. Fetch Computer Data" << endl;
        cout << "5. Exit" << endl;
        cin >> option;

        if (option == 1) {
            fetchUserData(ld, base_dn, rc);
        } else if (option == 2) {
            fetchOUData(ld, base_dn, rc);
        } else if (option == 3) {
            fetchGroupData(ld, base_dn, rc);
        } else if (option == 4) {
            fetchComputerData(ld, base_dn, rc);
        } else if (option == 5) {
            break;  // Exit the loop
        } else {
            cout << "Invalid option. Try again." << endl;
        }
    }