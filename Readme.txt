
1 - start the LDAP 
    * cd agent
    * g++ -I/usr/include -L/usr/lib/x86_64-linux-gnu agent.cpp -o agent -lldap -llber -lmysqlclient -lmysqlcppconn
    * ./agent

2 - start the server
    * cd backend
    * run tamcat

3 - start the client
    * cd frontend 
    * npm start (or) ember serve