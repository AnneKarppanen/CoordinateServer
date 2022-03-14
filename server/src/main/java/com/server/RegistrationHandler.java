package com.server;

import com.sun.net.httpserver.HttpHandler;

import org.json.JSONException;
import org.json.JSONObject;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class RegistrationHandler implements HttpHandler {

    private final UserAuthenticator userAuth;

    public RegistrationHandler(UserAuthenticator userAuth) {

        this.userAuth = userAuth;

    }

    @Override
    public void handle(HttpExchange t) throws IOException {

        System.out.println("Request handled in thread " + Thread.currentThread().getId());

        if (t.getRequestMethod().equalsIgnoreCase("POST")) {
            String responseMessage = new String();
            int responseCode = handlePostRequest(t, responseMessage);
            handleResponsePOST(t, responseCode, responseMessage);

        } else {

            handleResponse(t, "Not supported");

        }
    }

    private int handlePostRequest(HttpExchange exchange, String responseMessage) {

        int responseCode = 0;
        String requestBody = null;
        Headers headers = exchange.getRequestHeaders();
        String contentType = "";
        Boolean isJSON = false;

        if (headers.containsKey("Content-Type")) {
            contentType = headers.get("Content-Type").get(0);

            if (contentType.equalsIgnoreCase("application/json")) {
                isJSON = true;
            }
        }

        if (!isJSON) {
            responseCode = 415;
            responseMessage = "Content-Type not valid. Can only process JSON requests";
            return responseCode;
        }

        // Reads message from client to requestBody variable using Buffered Reader
        // within try-with-resources block. Checks if user can be registered. If user
        // cannot be registered
        // saves an errorMessage.

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            requestBody = reader.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            responseCode = 500;
            responseMessage = "Internal server error.";
            return responseCode;
        }

        if (requestBody == null || requestBody.length() == 0) {
            responseCode = 400;
            responseMessage = "Cannot register a new user without user credentials. Please provide username, password and email.";
            return responseCode;
        }

        JSONObject newUser = null;

        try {
            newUser = new JSONObject(requestBody);
        } catch (JSONException e) {
            responseCode = 400;
            responseMessage = "JSON parse error. User registration failed.";
            return responseCode;
        }

        String userName = newUser.getString("username");
        String password = newUser.getString("password");
        String email = newUser.getString("email");

        if (userName == null || password == null || email == null) {
            responseCode = 422;
            responseMessage = "Cannot register a user with incomplete credentials.";
            return responseCode;
        }

        if (userName.length() < 1 || password.length() < 1) {
            responseCode = 422;
            responseMessage = "Username and password must have at least 1 character.";
            return responseCode;
        }

        if (email.length() == 0) {
            responseCode = 422;
            responseMessage = "Cannot register a user without email.";
            return responseCode;
        }

        try {
            boolean userAdded = userAuth.addUser(new User(userName, password, email));
            if (userAdded) {
                responseCode = 200;
                responseMessage = "User registered.";
            } else {
                responseCode = 409;
                responseMessage = "Cannot register user";
            }
        } catch (Exception e) {
            responseMessage = e.getMessage();
            responseCode = 500;
        }

        return responseCode;

    }

    private void handleResponsePOST(HttpExchange exchange, int responseCode, String responseMessage)
            throws IOException {
        try {
            byte[] bytes = responseMessage.getBytes("UTF-8");
            exchange.sendResponseHeaders(responseCode, bytes.length);
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(bytes);
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

    private void handleResponse(HttpExchange exchange, String response) throws IOException {

        try {
            byte[] bytes = response.getBytes("UTF-8");
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
