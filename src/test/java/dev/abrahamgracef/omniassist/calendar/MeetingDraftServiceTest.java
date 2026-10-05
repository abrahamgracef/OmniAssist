package dev.abrahamgracef.omniassist.calendar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MeetingDraftServiceTest {

    private MeetingDraftService service;

    @BeforeEach
    void setUp() {
        service = new MeetingDraftService();
    }

    @Test
    void testCreateAndGetDraft() {
        MeetingDraft draft = service.create(
                "Sprint Planning",
                "Discuss sprint goals",
                "Google Meet",
                "2026-10-06T10:00:00Z",
                "2026-10-06T11:00:00Z",
                List.of("team@example.com")
        );

        assertThat(draft).isNotNull();
        assertThat(draft.id()).isNotNull();
        assertThat(draft.title()).isEqualTo("Sprint Planning");
        assertThat(draft.attendees()).containsExactly("team@example.com");

        MeetingDraft fetched = service.get(draft.id());
        assertThat(fetched).isEqualTo(draft);
    }

    @Test
    void testUpdateDraft() {
        MeetingDraft draft = service.create(
                "Design Sync",
                "UI review",
                "Room 2",
                "2026-10-06T14:00:00Z",
                "2026-10-06T15:00:00Z",
                List.of("designer@example.com")
        );

        MeetingDraft updated = service.update(
                draft.id(),
                "Updated Design Sync",
                "New agenda",
                "Google Meet",
                "2026-10-06T15:00:00Z",
                "2026-10-06T16:00:00Z",
                List.of("designer@example.com", "lead@example.com")
        );

        assertThat(updated.title()).isEqualTo("Updated Design Sync");
        assertThat(updated.description()).isEqualTo("New agenda");
        assertThat(updated.attendees()).hasSize(2);
        assertThat(service.get(draft.id()).title()).isEqualTo("Updated Design Sync");
    }

    @Test
    void testTakeForSchedulingAndRestore() {
        MeetingDraft draft = service.create(
                "Architecture Review",
                "Review RFC",
                "Google Meet",
                "2026-10-07T10:00:00Z",
                "2026-10-07T11:00:00Z",
                List.of()
        );

        MeetingDraft taken = service.takeForScheduling(draft.id());
        assertThat(taken).isEqualTo(draft);

        // Cannot take or get again
        assertThatThrownBy(() -> service.get(draft.id()))
                .isInstanceOf(IllegalArgumentException.class);

        // Restore if scheduling fails
        service.restore(taken);
        assertThat(service.get(draft.id())).isEqualTo(draft);
    }

    @Test
    void testDeleteDraft() {
        MeetingDraft draft = service.create(
                "1:1 Sync",
                "Catch up",
                "Room 1",
                "2026-10-08T09:00:00Z",
                "2026-10-08T09:30:00Z",
                List.of()
        );

        service.delete(draft.id());
        assertThatThrownBy(() -> service.get(draft.id()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testConsumeLatestDraft() {
        MeetingDraft draft = service.create(
                "Demo",
                "Show progress",
                "Main Stage",
                "2026-10-09T16:00:00Z",
                "2026-10-09T17:00:00Z",
                List.of()
        );

        MeetingDraft consumed = service.consumeLatestDraft();
        assertThat(consumed).isEqualTo(draft);

        // Second consume returns null
        assertThat(service.consumeLatestDraft()).isNull();
    }
}
