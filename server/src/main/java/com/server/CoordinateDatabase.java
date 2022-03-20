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
import java.util.Base64;
import java.security.SecureRandom;
import org.apache.commons.codec.digest.Crypt;

public class CoordinateDatabase {

    private String dbName = "coordinateDB.db";
    private Connection dbConnection = null;
    private static CoordinateDatabase dbInstance = null;
    private SecureRandom random;

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
        this.random = new SecureRandom();
    }

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
            String createUserTable = "CREATE TABLE user (username VARCHAR(50) PRIMARY KEY, password VARCHAR(150) NOT NULL, salt VARCHAR (100) NOT NULL, email VARCHAR(50) NOT NULL)";
            String createCoordinatetable = "CREATE TABLE coordinate (coordinateID INTEGER PRIMARY KEY, time INTEGER, latitude REAL NOT NULL, longitude REAL NOT NULL, user VARCHAR(50) REFERENCES user(username), description VARCHAR(1024))";
            String createCommentTable = "CREATE TABLE comment (commentID INTEGER PRIMARY KEY, commenttime INTEGER NOT NULL, content VARCHAR(1024) NOT NULL, coordinateID INTEGER REFERENCES coordinate(coordinateID))";
            try {
                Statement createStatement1 = dbConnection.createStatement();
                createStatement1.executeUpdate(createUserTable);
                createStatement1.close();
                Statement createStatement2 = dbConnection.createStatement();
                createStatement2.executeUpdate(createCoordinatetable);
                createStatement2.close();
                Statement createStatement3 = dbConnection.createStatement();
                createStatement3.executeUpdate(createCommentTable);
                createStatement3.close();
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

    public int addUserToDB(User user) throws SQLException, IllegalArgumentException {
        byte bytes[] = new byte[13];
        random.nextBytes(bytes);
        String saltBytes = new String(Base64.getEncoder().encode(bytes));
        String salt = "$6$" + saltBytes;
        String hashedPassword = Crypt.crypt(user.getPassword(), salt);
        String statementString = "INSERT INTO user VALUES('" + user.getUsername() + "', '" + hashedPassword + "', '"
                + salt + "', '" + user.getEmail() + "')";
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
            String passwordInDB = rs.getString("password");
            String cryptedPassword = Crypt.crypt(password, passwordInDB);
            if (passwordInDB.equals(cryptedPassword)) {
                result = true;
            }
        }
        queryStatement.close();

        return result;
    }

    public int addCoordinateToDB(UserCoordinate coordinate) throws SQLException {

        int coordinateCount = 0;
        Statement countStatement = dbConnection.createStatement();
        ResultSet countRs = countStatement.executeQuery("SELECT COUNT(coordinateID) FROM coordinate");
        coordinateCount = countRs.getInt(1);
        countRs.close();
        String statementString = "INSERT INTO coordinate VALUES('" + coordinateCount + "','"
                + coordinate.timestampAsLong() + "', '"
                + coordinate.getLatitude() + "', '" + coordinate.getLongitude() + "', '" + coordinate.getNick() + "', '"
                + coordinate.getDescription() + "')";
        Statement insertStatement = dbConnection.createStatement();
        int result = insertStatement.executeUpdate(statementString);
        insertStatement.close();
        return result;
    }

    public int addCommentToDB(Comment comment) throws SQLException {
        int commentCount = 0;
        Statement countStatement = dbConnection.createStatement();
        ResultSet countRs = countStatement.executeQuery("SELECT COUNT(commentID) FROM comment");
        commentCount = countRs.getInt(1);
        countRs.close();
        String statementString = "INSERT INTO comment VALUES('" + commentCount + "','" + comment.timestampAsLong()
                + "', '" + comment.getContent() + "', '" + comment.getCoordinateId() + "')";
        Statement insertStatement = dbConnection.createStatement();
        int result = insertStatement.executeUpdate(statementString);
        insertStatement.close();
        return result;
    }

    public ArrayList<UserCoordinate> getCoordinatesFromDB() throws SQLException {
        ArrayList<UserCoordinate> coordinateResult = new ArrayList<>();
        String queryString = "SELECT coordinate.coordinateID, coordinate.time, coordinate.latitude, coordinate.longitude, coordinate.user, coordinate.description, comment.commenttime, comment.content FROM coordinate LEFT JOIN comment ON comment.coordinateID = coordinate.coordinateID ORDER BY coordinate.coordinateID";
        Statement queryStatement = dbConnection.createStatement();
        ResultSet rs = queryStatement.executeQuery(queryString);
        coordinateResult = processCombinedResultSet(rs);
        queryStatement.close();

        return coordinateResult;
    }

    public ArrayList<UserCoordinate> getCoordinatesByUserFromDB(String userNick) throws SQLException {
        ArrayList<UserCoordinate> queryResult = new ArrayList<>();
        String queryString = "SELECT coordinate.coordinateID, coordinate.time, coordinate.latitude, coordinate.longitude, coordinate.user, coordinate.description, comment.commenttime, comment.content FROM coordinate LEFT JOIN comment ON coordinate.coordinateID = comment.coordinateID WHERE coordinate.user = '"
                + userNick + "' ORDER BY coordinate.coordinateID";
        // System.out.println(queryString);
        Statement queryStatement = dbConnection.createStatement();
        ResultSet rs = queryStatement.executeQuery(queryString);
        queryResult = processCombinedResultSet(rs);
        queryStatement.close();
        return queryResult;
    }

    public ArrayList<UserCoordinate> processCombinedResultSet(ResultSet rs) throws SQLException {
        ArrayList<UserCoordinate> coordinates = new ArrayList<>();
        int IdOfLastCoordinate = -1;
        UserCoordinate lastCoordinate = null;
        UserCoordinate coordinateToAdd = new UserCoordinate();
        Comment commentToAdd = new Comment();
        while (rs.next()) {
            int coordinateID = rs.getInt("coordinateID");
            if (coordinateID == IdOfLastCoordinate) {
                long commentSent = rs.getLong("commenttime");
                ZonedDateTime timestamp = ZonedDateTime.ofInstant(Instant.ofEpochMilli(commentSent), ZoneOffset.UTC);
                String content = rs.getString("content");
                commentToAdd = new Comment(coordinateID, timestamp, content);
                lastCoordinate.addCommentToCoordinate(commentToAdd);
            } else {
                IdOfLastCoordinate = coordinateID;
                long sent = rs.getLong("time");
                ZonedDateTime timestamp = ZonedDateTime.ofInstant(Instant.ofEpochMilli(sent), ZoneOffset.UTC);
                Double latitude = rs.getDouble("latitude");
                Double longitude = rs.getDouble("longitude");
                String nick = rs.getString("user");
                String description = rs.getString("description");
                coordinateToAdd = new UserCoordinate(nick, latitude, longitude, timestamp, description,
                        coordinateID);
                long commentSent = rs.getLong("commenttime");
                String content = rs.getString("content");
                if (!(commentSent == 0 || content.equals(null))) {
                    ZonedDateTime timestamp2 = ZonedDateTime.ofInstant(Instant.ofEpochMilli(commentSent),
                            ZoneOffset.UTC);
                    commentToAdd = new Comment(coordinateID, timestamp2, content);
                    coordinateToAdd.addCommentToCoordinate(commentToAdd);
                }
                coordinates.add(coordinateToAdd);
                lastCoordinate = coordinateToAdd;
            }

        }
        return coordinates;
    }

    public ArrayList<UserCoordinate> getCoordinatesByTimeFromDB(long startTime, long endTime) throws SQLException {
        ArrayList<UserCoordinate> queryResult = new ArrayList<>();
        String queryString = "SELECT coordinate.coordinateID, coordinate.time, coordinate.latitude, coordinate.longitude, coordinate.user, coordinate.description, comment.commenttime, comment.content FROM coordinate LEFT JOIN comment ON comment.coordinateID = coordinate.coordinateID WHERE coordinate.time >= "
                + startTime + " AND coordinate.time <= " + endTime + " ORDER BY coordinate.coordinateID";
        Statement queryStatement = dbConnection.createStatement();
        ResultSet rs = queryStatement.executeQuery(queryString);
        queryResult = processCombinedResultSet(rs);
        queryStatement.close();
        return queryResult;
    }

    public void close() {
        try {
            dbConnection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
    }

}
