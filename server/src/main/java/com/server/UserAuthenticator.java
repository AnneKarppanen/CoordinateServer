package com.server;

import java.sql.SQLException;

public class UserAuthenticator extends com.sun.net.httpserver.BasicAuthenticator {

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
            e.printStackTrace();
        }

        return result;
    }

    public boolean addUser(User user) throws Exception {

        boolean result = false;
        CoordinateDatabase db = CoordinateDatabase.getInstance();
        boolean userRegistered = db.checkIfUserExists(user.getUsername());
        if (!userRegistered) {
            int addedRows = db.addUserToDB(user);
            if (addedRows == 1) {
                result = true;
            }
        }

        return result;
    }

}
