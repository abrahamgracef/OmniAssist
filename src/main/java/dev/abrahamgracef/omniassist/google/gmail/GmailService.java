package dev.abrahamgracef.omniassist.google.gmail;

import org.springframework.stereotype.Service;
import org.springframework.core.ParameterizedTypeReference;
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

        Map<String, Object> response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/users/me/messages")
                        .queryParam("maxResults", maxResults)
                        .build())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (response == null || response.get("messages") == null) {
            return List.of();
        }

        List<?> messages = (List<?>) response.get("messages");

        List<EmailSummary> emails = new ArrayList<>();

        for (Object item : messages) {
            Map<String, Object> message = asObjectMap(item);
            String id = requiredString(message, "id", "Gmail message");

            emails.add(getEmail(accessToken, id));
        }

        return emails;
    }

    public List<EmailSummary> getSentMessages(
            String accessToken,
            int maxResults) {

        Map<String, Object> response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/users/me/messages")
                        .queryParam("q", "in:sent")
                        .queryParam("maxResults", maxResults)
                        .build())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (response == null || response.get("messages") == null) {
            return List.of();
        }

        List<?> messages = (List<?>) response.get("messages");

        List<EmailSummary> emails = new ArrayList<>();
        for (Object item : messages) {
            Map<String, Object> message = asObjectMap(item);
            String id = requiredString(message, "id", "Gmail message");
            emails.add(getEmail(accessToken, id));
        }

        return emails;
    }

    public boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) return false;
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    public String replyToEmail(
            String accessToken,
            String messageId,
            String threadId,
            String to,
            String subject,
            String body) {

        String safeSubject = subject != null && subject.startsWith("Re:") ? subject : "Re: " + (subject != null ? subject : "");
        String mimeMessage =
                "To: " + to + "\r\n" +
                "Subject: " + safeSubject + "\r\n" +
                "In-Reply-To: <" + messageId + ">\r\n" +
                "References: <" + messageId + ">\r\n" +
                "Content-Type: text/plain; charset=UTF-8\r\n\r\n" +
                body;

        String encodedMessage = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(mimeMessage.getBytes(StandardCharsets.UTF_8));

        Map<String, String> requestBody = threadId != null && !threadId.isBlank()
                ? Map.of("raw", encodedMessage, "threadId", threadId)
                : Map.of("raw", encodedMessage);

        Map<String, Object> response = restClient.post()
                .uri("/users/me/messages/send")
                .header("Authorization", "Bearer " + accessToken)
                .body(requestBody)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (response == null || response.get("id") == null) {
            throw new IllegalStateException("Gmail did not return a message ID for reply");
        }

        return response.get("id").toString();
    }

    public String forwardEmail(
            String accessToken,
            String originalMessageId,
            String to,
            String noteBody) {

        EmailSummary original = getEmail(accessToken, originalMessageId);
        String subject = original.subject() != null && original.subject().startsWith("Fwd:")
                ? original.subject()
                : "Fwd: " + (original.subject() != null ? original.subject() : "Forwarded Message");

        String combinedBody = (noteBody != null ? noteBody : "") +
                "\r\n\r\n---------- Forwarded message ---------\r\n" +
                "From: " + original.from() + "\r\n" +
                "Subject: " + original.subject() + "\r\n\r\n" +
                original.snippet();

        return sendEmail(accessToken, to, subject, combinedBody);
    }

    public EmailSummary getEmail(
            String accessToken,
            String messageId) {

        Map<String, Object> response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/users/me/messages/{id}")
                        .queryParam("format", "metadata")
                        .queryParam("metadataHeaders", "From")
                        .queryParam("metadataHeaders", "Subject")
                        .build(messageId))
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (response == null) {
            throw new IllegalStateException("Unable to read Gmail message");
        }

        String snippet = stringValue(response.get("snippet"));

        Map<String, Object> payload = asObjectMap(response.get("payload"));

        String from = "";
        String subject = "";

        if (payload != null) {

            Object headersValue = payload.get("headers");
            if (headersValue instanceof List<?> headers) {
                for (Object headerValue : headers) {
                    Map<String, Object> header = asObjectMap(headerValue);
                    String name = stringValue(header.get("name"));
                    String value = stringValue(header.get("value"));

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

        Map<String, Object> response = restClient.post()
                .uri("/users/me/messages/send")
                .header(
                        "Authorization",
                        "Bearer " + accessToken
                )
                .body(requestBody)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (response == null || response.get("id") == null) {
            throw new IllegalStateException(
                    "Gmail did not return a message ID"
            );
        }

        return response.get("id").toString();
    }
    private static Map<String, Object> asObjectMap(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        map.forEach((key, entryValue) -> {
            if (key instanceof String stringKey) {
                result.put(stringKey, entryValue);
            }
        });
        return result;
    }

    private static String requiredString(Map<String, Object> map, String key, String context) {
        Object value = map.get(key);
        if (value instanceof String string && !string.isBlank()) {
            return string;
        }
        throw new IllegalStateException(context + " response did not include " + key);
    }

    private static String stringValue(Object value) {
        return value instanceof String string ? string : "";
    }
}