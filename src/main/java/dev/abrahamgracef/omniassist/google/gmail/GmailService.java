package dev.abrahamgracef.omniassist.google.gmail;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class GmailService {

    private final RestClient restClient;

    public GmailService(RestClient.Builder builder) {
        this.restClient = builder
                .baseUrl("https://gmail.googleapis.com/gmail/v1")
                .build();
    }

    public List<EmailSummary> getLatestEmails(
            String accessToken,
            int maxResults) {

        Map response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/users/me/messages")
                        .queryParam("maxResults", maxResults)
                        .build())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(Map.class);

        if (response == null || response.get("messages") == null) {
            return List.of();
        }

        List<Map<String, String>> messages =
                (List<Map<String, String>>) response.get("messages");

        List<EmailSummary> emails = new ArrayList<>();

        for (Map<String, String> message : messages) {

            String id = message.get("id");

            emails.add(getEmail(accessToken, id));
        }

        return emails;
    }

    private EmailSummary getEmail(
            String accessToken,
            String messageId) {

        Map response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/users/me/messages/{id}")
                        .queryParam("format", "metadata")
                        .queryParam("metadataHeaders", "From")
                        .queryParam("metadataHeaders", "Subject")
                        .build(messageId))
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(Map.class);

        if (response == null) {
            throw new IllegalStateException("Unable to read Gmail message");
        }

        String snippet =
                (String) response.getOrDefault("snippet", "");

        Map<String, Object> payload =
                (Map<String, Object>) response.get("payload");

        String from = "";
        String subject = "";

        if (payload != null) {

            List<Map<String, String>> headers =
                    (List<Map<String, String>>) payload.get("headers");

            if (headers != null) {

                for (Map<String, String> header : headers) {

                    String name = header.get("name");
                    String value = header.get("value");

                    if ("From".equalsIgnoreCase(name)) {
                        from = value;
                    }

                    if ("Subject".equalsIgnoreCase(name)) {
                        subject = value;
                    }
                }
            }
        }


        return new EmailSummary(
                messageId,
                from,
                subject,
                snippet
        );
    }
    public String sendEmail(
            String accessToken,
            String to,
            String subject,
            String body) {
        String mimeMessage =
                "To: " + to + "\r\n" +
                        "Subject: " + subject + "\r\n" +
                        "Content-Type: text/plain; charset=UTF-8\r\n" +
                        "\r\n" +
                        body;

        String encodedMessage = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                        mimeMessage.getBytes(StandardCharsets.UTF_8)
                );

        Map<String, String> requestBody =
                Map.of("raw", encodedMessage);

        Map response = restClient.post()
                .uri("/users/me/messages/send")
                .header(
                        "Authorization",
                        "Bearer " + accessToken
                )
                .body(requestBody)
                .retrieve()
                .body(Map.class);

        if (response == null || response.get("id") == null) {
            throw new IllegalStateException(
                    "Gmail did not return a message ID"
            );
        }

        return response.get("id").toString();
    }
    private String normalizeEmailBody(String body) {

        if (body == null) {
            return "";
        }

        // Normalize Windows/Mac line endings
        String normalized = body
                .replace("\r\n", "\n")
                .replace("\r", "\n");

        // Remove single line breaks inside paragraphs,
        // while preserving blank lines between paragraphs.
        String[] paragraphs = normalized.split("\\n\\s*\\n");

        return java.util.Arrays.stream(paragraphs)
                .map(paragraph -> paragraph
                        .replaceAll("\\s*\\n\\s*", " ")
                        .trim())
                .filter(paragraph -> !paragraph.isEmpty())
                .collect(java.util.stream.Collectors.joining("\r\n\r\n"));
    }
}