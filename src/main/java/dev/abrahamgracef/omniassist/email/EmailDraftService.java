package dev.abrahamgracef.omniassist.email;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EmailDraftService {

    private final Map<UUID, EmailDraft> drafts =
            new ConcurrentHashMap<>();

    private EmailDraft latestDraft;

    public synchronized EmailDraft create(
            String to,
            String subject,
            String body) {

        EmailDraft draft = new EmailDraft(
                UUID.randomUUID(),
                to,
                subject,
                body
        );

        drafts.put(draft.id(), draft);
        latestDraft = draft;

        return draft;
    }

    public EmailDraft get(UUID id) {

        EmailDraft draft = drafts.get(id);

        if (draft == null) {
            throw new IllegalArgumentException(
                    "Email draft not found or already sent."
            );
        }

        return draft;
    }

    public EmailDraft update(
            UUID id,
            String to,
            String subject,
            String body) {

        get(id);

        EmailDraft updated = new EmailDraft(
                id,
                to,
                subject,
                body
        );

        drafts.put(id, updated);

        return updated;
    }

    /*
     * Atomically removes the draft before sending.
     * This prevents two requests from sending the same draft.
     */
    public EmailDraft takeForSending(UUID id) {

        EmailDraft draft = drafts.remove(id);

        if (draft == null) {
            throw new IllegalArgumentException(
                    "Email draft not found or already sent."
            );
        }

        return draft;
    }

    public void restore(EmailDraft draft) {
        drafts.putIfAbsent(draft.id(), draft);
    }

    public void delete(UUID id) {
        drafts.remove(id);
    }

    public synchronized EmailDraft consumeLatestDraft() {

        EmailDraft draft = latestDraft;
        latestDraft = null;

        return draft;
    }
}