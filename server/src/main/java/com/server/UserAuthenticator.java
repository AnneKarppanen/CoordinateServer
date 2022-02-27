package com.server;

import java.sql.SQLException;

public class UserAuthenticator extends com.sun.net.httpserver.BasicAuthenticator {

    private String errorMessage;
    private int errorCode;

    public UserAuthenticator(String realm) {
        super("/coordinates");

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
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public int getErrorCode() {
        return errorCode;
    }

}
