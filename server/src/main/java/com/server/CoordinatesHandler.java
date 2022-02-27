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

    int responseCode;

    public CoordinatesHandler() {

    }

    @Override
    public void handle(HttpExchange t) throws IOException {

        try {

            if (t.getRequestMethod().equalsIgnoreCase("POST")) {

                handlePostRequest(t);
                handleResponsePOST(t);

            } else if (t.getRequestMethod().equalsIgnoreCase("GET")) {

                String response = handleGetRequest(t);
                handleResponseGET(t, response);

            } else {

                handleResponse(t, "Not supported");

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private String handleGetRequest(HttpExchange exchange) {

        String response = "";
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

                response = responseCoordinates.toString();
            }
        } catch (Exception e) {
            responseCode = 500;
        }

        return response;

    }

    private void handlePostRequest(HttpExchange exchange) {

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
            return;
        }

        // Reads message from client to requestBody variable using Buffered Reader
        // within try-with-resources block.
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            requestBody = reader.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            System.out.println(e.getMessage());
            responseCode = 500;
            return;
        }

        if (requestBody == null || requestBody.length() == 0) {
            responseCode = 400;
            return;
        }

        JSONObject newCoordinates = null;

        try {
            newCoordinates = new JSONObject(requestBody);
        } catch (JSONException e) {
            responseCode = 400;
            return;
        }

        String userName = newCoordinates.getString("username");
        String longitude = newCoordinates.getString("longitude");
        String latitude = newCoordinates.getString("latitude");
        String timestamp = newCoordinates.getString("sent");

        if (userName == null || longitude == null || latitude == null || timestamp == null) {
            responseCode = 422;
            return;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        ZonedDateTime sendTime = null;

        try {
            sendTime = ZonedDateTime.parse(timestamp, formatter);
        } catch (Exception e) {
            responseCode = 422;
            e.printStackTrace();
            return;
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

    }

    private void handleResponseGET(HttpExchange exchange, String response) throws IOException {

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

    private void handleResponsePOST(HttpExchange exchange) throws IOException {
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
