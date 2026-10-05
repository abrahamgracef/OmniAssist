package dev.abrahamgracef.omniassist.calendar;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MeetingDraftService {

    private final Map<UUID, MeetingDraft> drafts =
            new ConcurrentHashMap<>();

    private MeetingDraft latestDraft;

    public synchronized MeetingDraft create(
            String title,
            String description,
            String location,
            String startDateTime,
            String endDateTime,
            List<String> attendees) {

        MeetingDraft draft = new MeetingDraft(
                UUID.randomUUID(),
                title,
                description,
                location,
                startDateTime,
                endDateTime,
                attendees != null ? attendees : List.of()
        );

        drafts.put(draft.id(), draft);
        latestDraft = draft;

        return draft;
    }

    public MeetingDraft get(UUID id) {
        MeetingDraft draft = drafts.get(id);

        if (draft == null) {
            throw new IllegalArgumentException(
                    "Meeting draft not found or already scheduled."
            );
        }

        return draft;
    }

    public MeetingDraft update(
            UUID id,
            String title,
            String description,
            String location,
            String startDateTime,
            String endDateTime,
            List<String> attendees) {

        get(id);

        MeetingDraft updated = new MeetingDraft(
                id,
                title,
                description,
                location,
                startDateTime,
                endDateTime,
                attendees != null ? attendees : List.of()
        );

        drafts.put(id, updated);

        return updated;
    }

    /**
     * Atomically removes the draft before scheduling to prevent duplicate scheduling.
     */
    public MeetingDraft takeForScheduling(UUID id) {
        MeetingDraft draft = drafts.remove(id);

        if (draft == null) {
            throw new IllegalArgumentException(
                    "Meeting draft not found or already scheduled."
            );
        }

        return draft;
    }

    public void restore(MeetingDraft draft) {
        drafts.putIfAbsent(draft.id(), draft);
    }

    public void delete(UUID id) {
        drafts.remove(id);
    }

    public synchronized MeetingDraft consumeLatestDraft() {
        MeetingDraft draft = latestDraft;
        latestDraft = null;
        return draft;
    }
}
