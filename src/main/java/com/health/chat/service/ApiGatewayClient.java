package com.health.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.health.chat.model.AuthResult;
import com.health.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

@Service
public class ApiGatewayClient {

    @Value("${api.gateway.url:http://localhost:8080}")
    private String apiGatewayUrl;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ApiGatewayClient() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public AuthResult authenticate(String username, String password) throws Exception {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("username", username);
        requestBody.put("password", password);

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiGatewayUrl + "/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), AuthResult.class);
        } else {
            AuthResult errorResult = new AuthResult();
            errorResult.setSuccess(false);
            errorResult.setErrorMessage("認証に失敗しました");
            return errorResult;
        }
    }

    public void logout(String token) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiGatewayUrl + "/auth/logout"))
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public ChatResponse sendMessage(String userId, String message, String token) throws Exception {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("userId", userId);
        requestBody.put("message", message);

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiGatewayUrl + "/chat"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), ChatResponse.class);
        } else {
            ChatResponse errorResponse = new ChatResponse();
            errorResponse.setResponseText("サーバーエラーが発生しました");
            return errorResponse;
        }
    }
}
