package com.example.slackbot.adapters.secondary;

import com.example.slackbot.domain.DailyStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.List;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.ArrayList;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.HttpHeaders;
import reactor.core.publisher.Mono;

@Component
public class CloudflareKVAdapter {
    private final WebClient client;
    private final String accountId;
    private final String namespaceId;

    public CloudflareKVAdapter(
            @Value("${cloudflare.apiToken}") String apiToken,
            @Value("${cloudflare.accountId}") String accountId,
            @Value("${cloudflare.namespaceId}") String namespaceId) {
        this.accountId = accountId;
        this.namespaceId = namespaceId;
        this.client = WebClient.builder()
            .baseUrl("https://api.cloudflare.com/client/v4")
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
            .build();
    }

    public void storeDailyStatus(DailyStatus status) {
        String jsonInputString = "{\"availability\": \"" + status.getAvailability() + "\", \"tasks\": \"" + status.getTasks() + "\", \"notes\": \"" + status.getNotes() + "\", \"date\": \"" + status.getDate() + "\"}";

        Mono<Void> save = client.put()
            .uri("/accounts/{acct}/storage/kv/namespaces/{ns}/values/{key}", accountId, namespaceId, status.getDeveloperId())
            .bodyValue(jsonInputString)
            .retrieve()
            .bodyToMono(Void.class);

        save.subscribe();
    }

    public List<DailyStatus> retrieveDailyStatuses(String date) {
        List<DailyStatus> statuses = new ArrayList<>();

        client.get()
            .uri("/accounts/{acct}/storage/kv/namespaces/{ns}/values?prefix={date}", accountId, namespaceId, date)
            .retrieve()
            .bodyToMono(String.class)
            .doOnNext(response -> {
                // Parse response to extract DailyStatus objects and add to statuses list
            })
            .block();

        return statuses;
    }
    
    /**
     * Get a value from Cloudflare KV
     * @param key The key to retrieve
     * @return The value associated with the key, or null if not found
     */
    public String get(String key) {
        return client.get()
            .uri("/accounts/{acct}/storage/kv/namespaces/{ns}/values/{key}", accountId, namespaceId, key)
            .retrieve()
            .bodyToMono(String.class)
            .onErrorReturn(null)  // Return null on error
            .block();
    }
    
    /**
     * Put a value into Cloudflare KV
     * @param key The key to store
     * @param value The value to store
     */
    public void put(String key, String value) {
        client.put()
            .uri("/accounts/{acct}/storage/kv/namespaces/{ns}/values/{key}", accountId, namespaceId, key)
            .bodyValue(value)
            .retrieve()
            .bodyToMono(Void.class)
            .subscribe();
    }
    
    /**
     * Delete a value from Cloudflare KV
     * @param key The key to delete
     */
    public void delete(String key) {
        client.delete()
            .uri("/accounts/{acct}/storage/kv/namespaces/{ns}/values/{key}", accountId, namespaceId, key)
            .retrieve()
            .bodyToMono(Void.class)
            .subscribe();
    }
} 