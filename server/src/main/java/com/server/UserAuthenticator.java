package com.server;

import java.sql.SQLException;
import java.util.ArrayList;

public class UserAuthenticator extends com.sun.net.httpserver.BasicAuthenticator {

    //private ArrayList<User> users = null;
    private String errorMessage;
    private int errorCode;

    public UserAuthenticator(String realm) {
        super("/coordinates");
        //this.users = new ArrayList<>();
        //users.add(new User("dummy", "password", "test@mail"));
    }

    @Override
    public boolean checkCredentials(String userName, String password) {
        boolean result = false;
        CoordinateDatabase db = CoordinateDatabase.getInstance();
        try {
            result = db.checkIfUsernameMatchesPassword(userName, password);
        } catch (Exception e) {
            errorMessage = e.getMessage();
            errorCode = 500;
        } 

        return result;
        /*for (User user : users) {
            if (user.getUsername().equals(userName) && user.getPassword().equals(password)) {
                return true;
            }
        }
        return false;*/
    }

    public boolean addUser(User user) {

        boolean result = false;
        CoordinateDatabase db = CoordinateDatabase.getInstance();
        try {
            boolean userRegistered = db.checkIfUserExists(user.getUsername());
            if (!userRegistered) {
                int addedRows = db.addUserToDB(user);
                if (addedRows == 1) {
                    result = true;
                }
            } else {
                errorMessage = "User already registered.";
                errorCode = 403;
            }
        } catch (SQLException e) {
            errorMessage = e.getMessage();
            errorCode = 500;
        }

        return result;

        /*
         * for (User user: users) {
         * if (user.getUsername().equals(userName)) {
         * errorMessage = "User already registered.";
         * errorCode = 403;
         * return false;
         * }
         * }
         * 
         * Boolean result = users.add(new User(userName, password, email));
         */
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public int getErrorCode() {
        return errorCode;
    }

}
