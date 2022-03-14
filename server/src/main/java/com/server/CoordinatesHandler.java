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

                int responseCode = handlePostRequest(t);
                handleResponsePOST(t, responseCode);

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

            if (queryResult.isEmpty()) {
                responseCode = 204;
            } else {
                JSONArray responseCoordinates = new JSONArray();
                for (UserCoordinate coordinate : queryResult) {
                    JSONObject obj = new JSONObject();
                    obj.put("username", coordinate.getNick());
                    obj.put("longitude", coordinate.getLongitude());
                    obj.put("latitude", coordinate.getLatitude());
                    obj.put("sent", coordinate.timestampToString());
                    responseCoordinates.put(obj);
                }

                response.append(responseCoordinates.toString());
                responseCode = 200;
            }
        } catch (Exception e) {
            responseCode = 500;
        }

        return responseCode;

    }

    private int handlePostRequest(HttpExchange exchange) {

        int responseCode = 0;
        String requestBody = null;
        Headers headers = exchange.getRequestHeaders();
        String contentType = "";
        boolean isJSON = false;
        CoordinateDatabase db = CoordinateDatabase.getInstance();

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

        // Reads message from client to requestBody variable using Buffered Reader
        // within try-with-resources block.
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

        JSONObject newCoordinates = null;
        String userName = null;
        Double longitude = null;
        Double latitude = null;
        String timestamp = null;

        try {
            newCoordinates = new JSONObject(requestBody);
            userName = newCoordinates.getString("username");
            longitude = newCoordinates.getDouble("longitude");
            latitude = newCoordinates.getDouble("latitude");
            timestamp = newCoordinates.getString("sent");
        } catch (JSONException e) {
            responseCode = 400;
            return responseCode;
        }

        if (userName == null || longitude.isNaN() || latitude.isNaN() || timestamp == null) {
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
            int coordinateAdded = db.addCoordinateToDB(new UserCoordinate(userName, latitude, longitude, sendTime));
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

    private void handleResponsePOST(HttpExchange exchange, int responseCode) throws IOException {
        try {
            exchange.sendResponseHeaders(responseCode, -1);
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
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
