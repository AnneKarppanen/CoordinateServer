This project is the course assignment for Programming 3 course. 
It is a server API that receives coordinates from a client. The client is not part of the implementation. 
This is an ongoing project with final deadline on March 20. 
The next steps include expanding and improving the functionalities that the API provides and creating JUnit tests. 
The server only accepts requests and sends coordinates in JSON. The users need to register to be able to post and get coordinates. 
The implementation consists of the following classes. 

Server
This is the main class of the implementation. It creates and starts the server and sets up SSLContext, CoordinateDatabase, 
UserAuthenticator and handlers for user registration and posting and getting coordinates. 

CoordinateDatabase
This class handles all interactions with the database. 
The database itself is an SQLite database that stores the user credentials and posted coordinates in a file.
When the server is started, the program checks if the database file exists and creates one if it has not been created yet.
The database implementation uses the singleton design pattern.  

UserAuthenticator
This class is used to add new users and to check the credentials of the existing users.
 
RegistrationHandler calls UserAuthenticator when it receives a request to register a new user and UserAuthenticator checks with the CoordinateDatabase 
if the user exists and only adds a new user if the same user does not exist yet in the database. UserAuthenticator lets the RegistrationHandler know if the new user was added or not.

Check credentials method is called when the UserAuthenticator's realm receives incoming requests. 
UserAuthenticator class calls for CoordinateDatabase and checks that the user that sent the request is registered and entered the correct password.
If not, the request is not carried out.    

RegistrationHandler
This class handles requests that are received by the registration realm. It processes the JSON POST requests and calls for UserAuthenticator to perform the registration.
It also sends the response to the client after the registration has been processed.

CoordinatesHandler
This class handles requests that are received by the coordinates realm. It processes the JSON POST and GET requests and calls for CoordinateDatabase to complete the transactions.
It also sends the response to the client after the request has been processed.

User
This class is used to encapsulate username, password and email when passing the user information between the classes. 

UserCoordinator
This class is used to encapsulate username, latitude, longitude and timestamp when passing the coordinate information between the classes. 
 
 
