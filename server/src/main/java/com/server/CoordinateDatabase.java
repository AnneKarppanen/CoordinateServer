package com.server;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;

public class CoordinateDatabase {

    private String dbName = "coordinateDB.db";
    private Connection dbConnection = null;
    private static CoordinateDatabase dbInstance = null;

    public static synchronized CoordinateDatabase getInstance() {
        if (null == dbInstance) {
            dbInstance = new CoordinateDatabase();
        }

        return dbInstance;
    }

    private CoordinateDatabase() {
        try {
            open(dbName);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 
     * @param dbName
     * @throws SQLException
     */
    public void open(String dbName) throws SQLException {

        boolean dbFileExists = false;
        String jdbcAddress = null;

        if (dbName != null) {
            File fileToTest = new File(dbName);
            if (fileToTest.exists() && !(fileToTest.isDirectory())) {
                dbFileExists = true;
            }
        }

        jdbcAddress = "jdbc:sqlite:" + dbName;
        dbConnection = DriverManager.getConnection(jdbcAddress);

        if (!dbFileExists) {
            initializeDatabase();
        }

    }

    private boolean initializeDatabase() {
        if (null != dbConnection) {
            String createUserTable = "CREATE TABLE user (username VARCHAR(50) PRIMARY KEY, password VARCHAR(50) NOT NULL, email VARCHAR(50) NOT NULL)";
            String createCoordinatetable = "CREATE TABLE coordinate (time INTEGER PRIMARY KEY, latitude VARCHAR(50) NOT NULL, longitude VARCHAR(50) NOT NULL, user VARCHAR(50) REFERENCES user(username))";
            try {
                Statement createStatement1 = dbConnection.createStatement();
                createStatement1.executeUpdate(createUserTable);
                createStatement1.close();
                Statement createStatement2 = dbConnection.createStatement();
                createStatement2.executeUpdate(createCoordinatetable);
                createStatement2.close();
                System.out.println("DB created");
                return true;

            } catch (SQLException e) {
                e.printStackTrace();
                System.out.println(e.getErrorCode());
            }

        }

        return false;

    }

    public boolean checkIfUserExists(String username) throws SQLException {
        boolean result = true;
        String queryString = "SELECT username FROM user WHERE username = '" + username + "'";

        Statement queryStatement = dbConnection.createStatement();
        ResultSet rs = queryStatement.executeQuery(queryString);
        if (!rs.next()) {
            result = false;
        }
        queryStatement.close();

        return result;

    }

    public int addUserToDB(User user) throws SQLException{
        String statementString = "INSERT INTO user VALUES('" + user.getUsername() + "', '" + user.getPassword() +  "', '" + user.getEmail() + "')";
        Statement insertStatement = dbConnection.createStatement();
        int result = insertStatement.executeUpdate(statementString);
        insertStatement.close();
        return result;
    }

    public boolean checkIfUsernameMatchesPassword(String username, String password) throws SQLException {
        boolean result = false;
        String queryString = "SELECT password FROM user WHERE username = '" + username + "'";
        Statement queryStatement = dbConnection.createStatement();
        ResultSet rs = queryStatement.executeQuery(queryString);
        if (!rs.next()) {
            result = false;
        } else {
            String passwordInB = rs.getString("password");
            if (password.equals(passwordInB)) {
                result = true;
            }
        }
        queryStatement.close();

        return result;
    }

    public int addCoordinateToDB(UserCoordinate coordinate) throws SQLException{
        String statementString = "INSERT INTO coordinate VALUES('" + coordinate.timestampAsLong() + "', '" + coordinate.getLatitude() +  "', '" + coordinate.getLongitude() + "', '" + coordinate.getNick() + "')";
        Statement insertStatement = dbConnection.createStatement();
        int result = insertStatement.executeUpdate(statementString);
        insertStatement.close();
        return result;
    }

    public ArrayList<UserCoordinate> getCoordinatesFromDB() throws SQLException {
        ArrayList<UserCoordinate> queryResult = new ArrayList<>();
        String queryString = "SELECT * FROM coordinate";
        Statement queryStatement = dbConnection.createStatement();
        ResultSet rs = queryStatement.executeQuery(queryString);
        while (rs.next()) { 
            long sent = rs.getInt("time");
            ZonedDateTime timestamp = ZonedDateTime.ofInstant(Instant.ofEpochMilli(sent), ZoneOffset.UTC);
            String latitude = rs.getString("latitude");
            String longitude = rs.getString("longitude");
            String nick = rs.getString("user");
            queryResult.add(new UserCoordinate(nick, latitude, longitude, timestamp));
    
        }
        queryStatement.close();

        return queryResult;
    }

}
