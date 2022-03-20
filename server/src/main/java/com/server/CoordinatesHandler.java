package com.server;

import com.sun.net.httpserver.HttpHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class CoordinatesHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange t) throws IOException {

        try {

            if (t.getRequestMethod().equalsIgnoreCase("POST")) {

                StringBuilder response = new StringBuilder();
                int responseCode = handlePostRequest(t, response);
                handleResponsePOST(t, response.toString(), responseCode);

            } else if (t.getRequestMethod().equalsIgnoreCase("GET")) {
                StringBuilder response = new StringBuilder();
                int responseCode = handleGetRequest(t, response);
                handleResponseGET(t, response.toString(), responseCode);

            } else {

                handleResponse(t, "Not supported");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private int handleGetRequest(HttpExchange exchange, StringBuilder response) {
        int responseCode = 0;
        CoordinateDatabase db = CoordinateDatabase.getInstance();
        ArrayList<UserCoordinate> queryResult = new ArrayList<>();

        try {
            queryResult = db.getCoordinatesFromDB();
            responseCode = queryResultToJSON(queryResult, response);
        } catch (Exception e) {
            responseCode = 500;
        }

        return responseCode;

    }

    private int handlePostRequest(HttpExchange exchange, StringBuilder response) {

        int responseCode = 0;
        String requestBody = null;
        Headers headers = exchange.getRequestHeaders();
        String contentType = "";
        boolean isJSON = false;

        if (headers.containsKey("Content-Type")) {
            contentType = headers.get("Content-Type").get(0);

            if (contentType.equalsIgnoreCase("application/json")) {
                isJSON = true;
            }
        }

        if (!isJSON) {
            responseCode = 415;
            return responseCode;
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            requestBody = reader.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            System.out.println(e.getMessage());
            responseCode = 500;
            return responseCode;
        }

        if (requestBody == null || requestBody.length() == 0) {
            responseCode = 400;
            return responseCode;
        }

        JSONObject coordinateRequest = null;
        try {
            coordinateRequest = new JSONObject(requestBody);
        } catch (JSONException e) {
            responseCode = 400;
            return responseCode;
        }

        String query = "";

        try {
            query = coordinateRequest.getString("query");
        } catch (NullPointerException e) {
        } catch (JSONException e) {
        }

        if (query.equals("user")) {
            responseCode = postWithUserQuery(coordinateRequest, response);
        } else if (query.equals("time")) {
            responseCode = postWithTimeQuery(coordinateRequest, response);
        } else if (query.equals("")) {
            responseCode = postNewCoordinate(coordinateRequest);
        } else {
            responseCode = 400;
        }

        return responseCode;

    }

    public int postWithUserQuery(JSONObject coordinateRequest, StringBuilder response) {

        int responseCode = 0;
        String userName = null;

        try {
            userName = coordinateRequest.getString("nickname");
        } catch (JSONException e) {
            responseCode = 400;
            return responseCode;
        }

        if (userName == null) {
            responseCode = 422;
            return responseCode;
        }

        ArrayList<UserCoordinate> queryResult = new ArrayList<>();
        CoordinateDatabase db = CoordinateDatabase.getInstance();

        try {
            queryResult = db.getCoordinatesByUserFromDB(userName);
            responseCode = queryResultToJSON(queryResult, response);
        } catch (SQLException e) {
            responseCode = 500;
        }

        return responseCode;
    }

    public int postWithTimeQuery(JSONObject coordinateRequest, StringBuilder response) {
        int responseCode = 0;
        String timestart = null;
        String timeend = null;

        try {
            timestart = coordinateRequest.getString("timestart");
            timeend = coordinateRequest.getString("timeend");
        } catch (JSONException e) {
            responseCode = 400;
            return responseCode;
        }

        if (timestart == null || timeend == null) {
            responseCode = 422;
            return responseCode;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        ZonedDateTime startTime = null;
        ZonedDateTime endTime = null;

        try {
            startTime = ZonedDateTime.parse(timestart, formatter);
            endTime = ZonedDateTime.parse(timeend, formatter);
        } catch (Exception e) {
            responseCode = 422;
            e.printStackTrace();
            return responseCode;
        }
        ArrayList<UserCoordinate> queryResult = new ArrayList<>();
        CoordinateDatabase db = CoordinateDatabase.getInstance();
        long sTime = startTime.toInstant().toEpochMilli();
        long eTime = endTime.toInstant().toEpochMilli();

        try {
            queryResult = db.getCoordinatesByTimeFromDB(sTime, eTime);
            responseCode = queryResultToJSON(queryResult, response);
        } catch (Exception e) {
            responseCode = 500;
        }

        return responseCode;
    }

    public int queryResultToJSON(ArrayList<UserCoordinate> queryResult, StringBuilder response) throws JSONException {

        int responseCode;
        if (queryResult.isEmpty()) {
            responseCode = 204;
        } else {
            JSONArray responseCoordinates = new JSONArray();
            for (UserCoordinate coordinate : queryResult) {
                JSONObject obj = new JSONObject();
                obj.put("id", coordinate.getCoordinateID());
                obj.put("username", coordinate.getNick());
                obj.put("longitude", coordinate.getLongitude());
                obj.put("latitude", coordinate.getLatitude());
                obj.put("sent", coordinate.timestampToString());
                if (!coordinate.getDescription().equals("nodata")) {
                    obj.put("description", coordinate.getDescription());
                }
                ArrayList<Comment> comments = coordinate.getComments();
                if (comments.size() > 0) {
                    JSONObject obj2 = null;
                    JSONArray commentsToCoordinate = new JSONArray();
                    for (Comment comment : comments) {
                        obj2 = new JSONObject();
                        obj2.put("comment", comment.getContent());
                        obj2.put("sent", comment.getTimestamp());
                        commentsToCoordinate.put(obj2);
                        System.out.println(obj2);
                        System.out.println();
                    }

                    obj.put("comments", commentsToCoordinate);
                }
                responseCoordinates.put(obj);
            }

            response.append(responseCoordinates.toString());
            responseCode = 200;
        }

        return responseCode;
    }

    public int postNewCoordinate(JSONObject coordinateRequest) {

        CoordinateDatabase db = CoordinateDatabase.getInstance();
        int responseCode = 0;
        String userName = null;
        Double longitude = null;
        Double latitude = null;
        String timestamp = null;
        String description = "";

        try {
            userName = coordinateRequest.getString("username");
            longitude = coordinateRequest.getDouble("longitude");
            latitude = coordinateRequest.getDouble("latitude");
            timestamp = coordinateRequest.getString("sent");
            try {
                description = coordinateRequest.getString("description");
            } catch (NullPointerException e) {
                description = "nodata";
            } catch (JSONException e) {
                description = "nodata";
            }
        } catch (JSONException e) {
            responseCode = 400;
            return responseCode;
        }

        if (userName == null || longitude.isNaN() || latitude.isNaN() || timestamp == null
                || description.length() > 1024) {
            responseCode = 422;
            return responseCode;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        ZonedDateTime sendTime = null;

        try {
            sendTime = ZonedDateTime.parse(timestamp, formatter);
        } catch (Exception e) {
            responseCode = 422;
            e.printStackTrace();
            return responseCode;
        }

        try {
            int coordinateAdded = db
                    .addCoordinateToDB(new UserCoordinate(userName, latitude, longitude, sendTime, description));
            if (coordinateAdded == 1) {
                responseCode = 200;
                System.out.println("Coordinate added");
            } else {
                responseCode = 500;
            }
        } catch (SQLException e) {
            responseCode = 500;
            e.printStackTrace();
        }

        return responseCode;

    }

    private void handleResponseGET(HttpExchange exchange, String response, int responseCode) throws IOException {
        try {

            if (response.equals("")) {
                exchange.sendResponseHeaders(responseCode, -1);
                OutputStream outputStream = exchange.getResponseBody();
                outputStream.flush();
                outputStream.close();
            } else {
                byte[] bytes = response.getBytes("UTF-8");
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(responseCode, bytes.length);
                OutputStream outputStream = exchange.getResponseBody();
                outputStream.write(bytes);
                outputStream.flush();
                outputStream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleResponsePOST(HttpExchange exchange, String response, int responseCode) throws IOException {
        try {

            if (response.equals("")) {
                exchange.sendResponseHeaders(responseCode, -1);
                OutputStream outputStream = exchange.getResponseBody();
                outputStream.flush();
                outputStream.close();
            } else {
                byte[] bytes = response.getBytes("UTF-8");
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(responseCode, bytes.length);
                OutputStream outputStream = exchange.getResponseBody();
                outputStream.write(bytes);
                outputStream.flush();
                outputStream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void handleResponse(HttpExchange exchange, String request) throws IOException {

        try {
            byte[] bytes = request.getBytes("UTF-8");
            exchange.sendResponseHeaders(400, bytes.length);
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(bytes);
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {

            e.printStackTrace();
        }

    }

}
