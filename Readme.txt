
1 - start the LDAP 
    * cd agent
    * g++ -I/usr/include -L/usr/lib/x86_64-linux-gnu agent.cpp -o agent -lldap -llber -lmysqlcppconn
    * ./agent

2 - start the server
    * cd project-backend
    * mvn clean install
    * mvn exec:java 

3 - start the client
    * cd frontend 
    * npm start (or) ember serve
