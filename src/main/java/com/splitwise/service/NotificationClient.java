package com.splitwise.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.splitwise.config.NotificationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationClient {

    private static final int MAX_ATTEMPTS = 3;
    private static final int MAX_BATCH_SIZE = 500; // limit of /internal/notifications/bulk

    private final RestClient notificationRestClient;
    private final NotificationProperties properties;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SendRequest(
            String userId,
            String type,
            Set<String> channels,
            Map<String, String> params,
            String title,
            String message,
            String referenceType,
            String referenceId,
            String idempotencyKey) {
    }

    public record BulkRequest(List<SendRequest> notifications) {
    }

    public void sendBulk(List<SendRequest> requests) {
        if (!properties.enabled() || requests == null || requests.isEmpty()) {
            return;
        }
        for (int from = 0; from < requests.size(); from += MAX_BATCH_SIZE) {
            int to = Math.min(from + MAX_BATCH_SIZE, requests.size());
            post(requests.subList(from, to));
        }
    }

    private void post(List<SendRequest> batch) {
        System.out.println("[NOTIFY] post() start: batchSize=" + batch.size()
                + " url=" + properties.baseUrl() + "/internal/notifications/bulk"
                + " keyLen=" + properties.internalApiKey().length());

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            System.out.println("[NOTIFY] attempt " + attempt + "/" + MAX_ATTEMPTS + " sending...");
            try {
                notificationRestClient.post()
                        .uri("/internal/notifications/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new BulkRequest(batch))
                        .retrieve()
                        .toBodilessEntity();
                System.out.println("[NOTIFY] attempt " + attempt + " SUCCESS: " + batch.size() + " notification(s) accepted");
                log.debug("Sent {} notification(s) to the notification service", batch.size());
                return;
            } catch (RestClientResponseException e) {
                System.out.println("[NOTIFY] attempt " + attempt + " HTTP ERROR: status=" + e.getStatusCode()
                        + " body=" + e.getResponseBodyAsString());
                if (e.getStatusCode().is4xxClientError()) {
                    log.error("Notification service rejected the request ({}): {}",
                            e.getStatusCode(), e.getResponseBodyAsString());
                    System.out.println("[NOTIFY] 4xx -> not retrying");
                    return; // our request is wrong; retrying won't help
                }
                log.warn("Notification service error {} (attempt {}/{})", e.getStatusCode(), attempt, MAX_ATTEMPTS);
            } catch (RestClientException e) {
                System.out.println("[NOTIFY] attempt " + attempt + " CONNECTION ERROR: " + e.getClass().getSimpleName()
                        + " - " + e.getMessage());
                log.warn("Notification service unreachable (attempt {}/{}): {}", attempt, MAX_ATTEMPTS, e.getMessage());
            }

            if (attempt < MAX_ATTEMPTS) {
                System.out.println("[NOTIFY] retrying in " + (attempt * 500) + " ms");
                if (!pause(attempt * 500L)) {
                    System.out.println("[NOTIFY] interrupted while waiting, stopping");
                    return;
                }
            }
        }
        System.out.println("[NOTIFY] GIVING UP after " + MAX_ATTEMPTS + " attempts; "
                + batch.size() + " notification(s) NOT delivered");
        log.error("Giving up after {} attempts; {} notification(s) were not delivered", MAX_ATTEMPTS, batch.size());
    }

    private boolean pause(long millis) {
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}