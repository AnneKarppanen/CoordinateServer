package com.server;

import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import org.json.JSONException;
import org.json.JSONObject;

import com.sun.net.httpserver.Headers;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

public class CommentHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange t) throws IOException {
        try {

            if (t.getRequestMethod().equalsIgnoreCase("POST")) {

                int responseCode = handlePostRequest(t);
                handleResponsePOST(t, responseCode);

            } else {

                handleResponse(t, "Not supported");

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private int handlePostRequest(HttpExchange exchange) {

        CoordinateDatabase db = CoordinateDatabase.getInstance();
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

        JSONObject commentRequest = null;
        try {
            commentRequest = new JSONObject(requestBody);
        } catch (JSONException e) {
            responseCode = 400;
            return responseCode;
        }

        int coordinateId = -1;
        String comment = "";
        String sent = "";

        try {
            coordinateId = commentRequest.getInt("id");
            comment = commentRequest.getString("comment");
            sent = commentRequest.getString("sent");
        } catch (JSONException e) {
            responseCode = 400;
        }

        if (coordinateId == -1 || sent == null || comment.length() < 1) {
            responseCode = 422;
            return responseCode;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        ZonedDateTime sendTime = null;

        try {
            sendTime = ZonedDateTime.parse(sent, formatter);
        } catch (Exception e) {
            responseCode = 422;
            e.printStackTrace();
            return responseCode;
        }

        try {
            int commentAdded = db.addCommentToDB(new Comment(coordinateId, sendTime, comment));
            if (commentAdded == 1) {
                responseCode = 200;
                System.out.println("Comment added");
            } else {
                responseCode = 500;
            }
        } catch (SQLException e) {
            responseCode = 500;
            e.printStackTrace();
        }

        return responseCode;

    }

    private void handleResponsePOST(HttpExchange exchange, int responseCode) throws IOException {
        try {
            exchange.sendResponseHeaders(responseCode, -1);
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.flush();
            outputStream.close();

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
