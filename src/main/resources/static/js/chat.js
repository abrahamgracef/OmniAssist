/* =========================================================
   OmniAssist Modern SaaS Frontend Engine (Veed.io Inspired)
   Zero Emojis • Vector Icons • Executive Dashboard Hub
   ========================================================= */

// Global State
let conversationId = null;
let currentUser = null;
let currentTaskFilter = "ALL";
let isTtsEnabled = localStorage.getItem("omni_tts") === "true";
let recognition = null;
let isRecording = false;
let currentQaDocId = null;
let currentBriefingRawText = "";

// DOM Elements
const form = document.getElementById("chat-form");
const input = document.getElementById("message-input");
const messages = document.getElementById("messages");
const sendButton = document.getElementById("send-button");
const suggestionChipsContainer = document.getElementById("suggestion-chips");
const themeToggleBtn = document.getElementById("theme-toggle-btn");
const voiceTtsBtn = document.getElementById("voice-tts-toggle-btn");
const micBtn = document.getElementById("mic-btn");

// Auth & Header
const signInButton = document.getElementById("google-signin");
const profileCard = document.getElementById("google-profile");
const headerSettingsBtn = document.getElementById("header-settings-btn");
const settingsDropdown = document.getElementById("settings-dropdown");
const settingsModal = document.getElementById("settings-modal");
const closeSettingsModalBtn = document.getElementById("close-settings-modal-btn");

// Initialize on Load
document.addEventListener("DOMContentLoaded", () => {
    initTheme();
    initVoiceMode();
    initNavigation();
    initHomeDashboard();
    initSettingsAndDropdown();
    initAuthAndProfile();
    initBriefingView();
    initConversations();
    initNotifications();
    initSuggestionChips();
    initTasksView();
    initCalendarView();
    initInboxView();
    initKnowledgeView();
});

/* =========================================================
   THEME TOGGLING (DEFAULT LIGHT)
   ========================================================= */
function initTheme() {
    const savedTheme = localStorage.getItem("omni_theme") || "light";
    applyTheme(savedTheme);

    if (themeToggleBtn) {
        themeToggleBtn.addEventListener("click", () => {
            const current = document.documentElement.getAttribute("data-theme") || "light";
            const target = current === "light" ? "dark" : "light";
            applyTheme(target);
            localStorage.setItem("omni_theme", target);
        });
    }
}

function applyTheme(theme) {
    document.documentElement.setAttribute("data-theme", theme);
    const themeSvg = document.getElementById("theme-icon-svg");
    if (themeSvg) {
        if (theme === "dark") {
            themeSvg.innerHTML = `<circle cx="12" cy="12" r="5"></circle><line x1="12" y1="1" x2="12" y2="3"></line><line x1="12" y1="21" x2="12" y2="23"></line><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"></line><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"></line><line x1="1" y1="12" x2="3" y2="12"></line><line x1="21" y1="12" x2="23" y2="12"></line><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"></line><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"></line>`;
        } else {
            themeSvg.innerHTML = `<path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"></path>`;
        }
    }
}

/* =========================================================
   VOICE MODE (SPEECH-TO-TEXT & TEXT-TO-SPEECH)
   ========================================================= */
function initVoiceMode() {
    updateTtsIcon();

    if (voiceTtsBtn) {
        voiceTtsBtn.addEventListener("click", () => {
            isTtsEnabled = !isTtsEnabled;
            localStorage.setItem("omni_tts", isTtsEnabled);
            updateTtsIcon();
            if (!isTtsEnabled && window.speechSynthesis) {
                window.speechSynthesis.cancel();
            }
        });
    }

    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (SpeechRecognition) {
        recognition = new SpeechRecognition();
        recognition.continuous = false;
        recognition.interimResults = true;
        recognition.lang = "en-US";

        recognition.onstart = () => {
            isRecording = true;
            if (micBtn) micBtn.classList.add("listening");
            const homeMic = document.getElementById("home-assistant-mic-btn");
            if (homeMic) homeMic.classList.add("listening");
        };

        recognition.onresult = (event) => {
            let transcript = "";
            for (let i = event.resultIndex; i < event.results.length; i++) {
                transcript += event.results[i][0].transcript;
            }
            if (input) input.value = transcript;
            const homeInput = document.getElementById("home-assistant-input");
            if (homeInput) homeInput.value = transcript;
        };

        recognition.onerror = (event) => {
            console.warn("Speech error:", event.error);
            stopRecording();
        };

        recognition.onend = () => {
            stopRecording();
        };

        if (micBtn) {
            micBtn.addEventListener("click", toggleRecording);
        }
        const homeMic = document.getElementById("home-assistant-mic-btn");
        if (homeMic) {
            homeMic.addEventListener("click", toggleRecording);
        }
    } else {
        if (micBtn) micBtn.style.display = "none";
        const homeMic = document.getElementById("home-assistant-mic-btn");
        if (homeMic) homeMic.style.display = "none";
    }
}

function toggleRecording() {
    if (isRecording) {
        recognition.stop();
        stopRecording();
    } else {
        try {
            recognition.start();
        } catch (e) {
            console.error("Speech start error:", e);
        }
    }
}

function stopRecording() {
    isRecording = false;
    if (micBtn) micBtn.classList.remove("listening");
    const homeMic = document.getElementById("home-assistant-mic-btn");
    if (homeMic) homeMic.classList.remove("listening");
}

function updateTtsIcon() {
    const ttsSvg = document.getElementById("tts-icon-svg");
    if (!ttsSvg) return;
    if (isTtsEnabled) {
        ttsSvg.innerHTML = `<polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"></polygon><path d="M19.07 4.93a10 10 0 0 1 0 14.14M15.54 8.46a5 5 0 0 1 0 7.07"></path>`;
    } else {
        ttsSvg.innerHTML = `<polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"></polygon><line x1="23" y1="9" x2="17" y2="15"></line><line x1="17" y1="9" x2="23" y2="15"></line>`;
    }
}

function speakText(text) {
    if (!isTtsEnabled || !window.speechSynthesis || !text) return;
    try {
        window.speechSynthesis.cancel();
        const plainText = text
            .replace(/[#*`_~]/g, "")
            .replace(/https?:\/\/\S+/g, "link")
            .replace(/\[([^\]]+)\]\([^)]+\)/g, "$1");
        const utterance = new SpeechSynthesisUtterance(plainText);
        utterance.rate = 1.05;
        window.speechSynthesis.speak(utterance);
    } catch (e) {
        console.error("TTS error:", e);
    }
}

/* =========================================================
   NAVIGATION / VIEW SWITCHING
   ========================================================= */
function initNavigation() {
    const navItems = document.querySelectorAll(".nav-item");
    const viewPanels = document.querySelectorAll(".view-panel");

    navItems.forEach(item => {
        item.addEventListener("click", () => {
            const view = item.getAttribute("data-view");
            if (!view) return;

            navItems.forEach(n => n.classList.remove("active"));
            item.classList.add("active");

            viewPanels.forEach(panel => {
                panel.style.display = panel.id === `view-${view}` ? "flex" : "none";
            });

            if (view === "home") loadHomeDashboard();
            if (view === "tasks") loadTasks();
            if (view === "calendar") loadCalendarEvents();
            if (view === "inbox") loadInboxMessages();
            if (view === "knowledge") loadKnowledgeNotes();
        });
    });

    // Global Search Shortcut (Ctrl+K)
    document.addEventListener("keydown", (e) => {
        if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === "k") {
            e.preventDefault();
            const searchBox = document.getElementById("global-search-input");
            if (searchBox) searchBox.focus();
        }
    });

    const globalSearch = document.getElementById("global-search-input");
    if (globalSearch) {
        globalSearch.addEventListener("keydown", (e) => {
            if (e.key === "Enter" && globalSearch.value.trim()) {
                switchToChatWithQuery(globalSearch.value.trim());
                globalSearch.value = "";
            }
        });
    }
}

function switchToView(viewName) {
    const navItem = document.querySelector(`.nav-item[data-view='${viewName}']`);
    if (navItem) navItem.click();
}

function switchToChatWithQuery(query) {
    switchToView("chat");
    input.value = query;
    form.dispatchEvent(new Event("submit"));
}

/* =========================================================
   VIEW 0: HOME / EXECUTIVE HUB DASHBOARD
   ========================================================= */
function initHomeDashboard() {
    // Quick Action Cards
    document.getElementById("card-new-chat")?.addEventListener("click", () => {
        switchToView("chat");
        startNewConversation();
    });

    document.getElementById("card-morning-briefing")?.addEventListener("click", () => {
        switchToView("chat");
        const briefingCard = document.getElementById("briefing-card");
        if (briefingCard) briefingCard.style.display = "block";
        loadMorningBriefing();
    });

    document.getElementById("card-auto-block")?.addEventListener("click", () => {
        triggerAutoTimeBlocking();
    });

    document.getElementById("card-process-notes")?.addEventListener("click", () => {
        switchToView("knowledge");
        const box = document.getElementById("meeting-minutes-box");
        if (box) box.style.display = "block";
    });

    // View more links
    document.getElementById("home-view-calendar-btn")?.addEventListener("click", () => switchToView("calendar"));
    document.getElementById("home-view-tasks-btn")?.addEventListener("click", () => switchToView("tasks"));
    document.getElementById("home-view-inbox-btn")?.addEventListener("click", () => switchToView("inbox"));

    // Assistant dock input
    const homeInput = document.getElementById("home-assistant-input");
    const homeSendBtn = document.getElementById("home-assistant-send-btn");

    function submitHomeAssistant() {
        const text = homeInput.value.trim();
        if (!text) return;
        switchToChatWithQuery(text);
        homeInput.value = "";
    }

    homeSendBtn?.addEventListener("click", submitHomeAssistant);
    homeInput?.addEventListener("keydown", (e) => {
        if (e.key === "Enter") submitHomeAssistant();
    });

    document.querySelectorAll(".dock-chip").forEach(chip => {
        chip.addEventListener("click", () => {
            const query = chip.getAttribute("data-home-query") || chip.textContent;
            switchToChatWithQuery(query);
        });
    });

    loadHomeDashboard();
}

async function loadHomeDashboard() {
    // 1. Load today's events on Home
    const eventsContainer = document.getElementById("home-events-container");
    if (eventsContainer) {
        try {
            const res = await fetch("/api/calendar/events?daysAhead=1&maxResults=5");
            if (res.ok) {
                const events = await res.json();
                if (events.length === 0) {
                    eventsContainer.innerHTML = "<p class='empty-state' style='padding:12px 0;'>No meetings scheduled for today.</p>";
                } else {
                    eventsContainer.innerHTML = "";
                    events.slice(0, 4).forEach(e => {
                        const el = document.createElement("div");
                        el.className = "event-item";
                        el.innerHTML = `
                            <div class="event-title">${escapeHtml(e.summary || "Untitled Event")}</div>
                            <div class="event-time">${e.start || ""} - ${e.end || ""}</div>
                        `;
                        eventsContainer.appendChild(el);
                    });
                }
            } else {
                eventsContainer.innerHTML = "<p class='empty-state' style='padding:12px 0;'>Sign in with Google to view today's calendar.</p>";
            }
        } catch (e) {
            eventsContainer.innerHTML = "<p class='empty-state' style='padding:12px 0;'>Connect Google to sync agenda.</p>";
        }
    }

    // 2. Load pending tasks on Home
    const tasksContainer = document.getElementById("home-tasks-container");
    if (tasksContainer) {
        try {
            const res = await fetch("/api/tasks/daily-planner");
            if (res.ok) {
                const tasks = await res.json();
                if (tasks.length === 0) {
                    tasksContainer.innerHTML = "<p class='empty-state' style='padding:12px 0;'>All caught up! No pending daily planner tasks.</p>";
                } else {
                    tasksContainer.innerHTML = "";
                    tasks.slice(0, 4).forEach(t => {
                        const el = document.createElement("div");
                        el.className = "task-card";
                        el.style.padding = "8px 12px";
                        el.innerHTML = `
                            <div class="task-left">
                                <span class="task-title">${escapeHtml(t.title)}</span>
                            </div>
                            <span class="priority-badge priority-${t.priority}">${t.priority}</span>
                        `;
                        tasksContainer.appendChild(el);
                    });
                }
            }
        } catch (e) {
            tasksContainer.innerHTML = "<p class='empty-state'>Failed to load tasks.</p>";
        }
    }

    // 3. Load unread emails triage on Home
    const triageContainer = document.getElementById("home-triage-container");
    if (triageContainer) {
        try {
            const res = await fetch("/api/gmail/triage");
            if (res.ok) {
                const data = await res.json();
                const list = data.triagedEmails || [];
                if (list.length === 0) {
                    triageContainer.innerHTML = "<p class='empty-state' style='padding:12px 0;'>Inbox Zero! No pending email action items.</p>";
                } else {
                    triageContainer.innerHTML = "";
                    list.slice(0, 3).forEach(item => {
                        const el = document.createElement("div");
                        el.className = "triage-card";
                        el.style.padding = "10px";
                        el.innerHTML = `
                            <div class="triage-card-header">
                                <span class="triage-card-subject">${escapeHtml(item.subject || '(No Subject)')}</span>
                                <span class="priority-badge priority-${item.urgency || 'MEDIUM'}">${item.urgency || 'MEDIUM'}</span>
                            </div>
                            <div class="triage-reason">${escapeHtml(item.reason || '')}</div>
                        `;
                        triageContainer.appendChild(el);
                    });
                }
            } else {
                triageContainer.innerHTML = "<p class='empty-state' style='padding:12px 0;'>Sign in with Google to view email triage.</p>";
            }
        } catch (e) {
            triageContainer.innerHTML = "<p class='empty-state' style='padding:12px 0;'>Connect Google to sync mail triage.</p>";
        }
    }
}

/* =========================================================
   SETTINGS & THREE-DOTS MENU
   ========================================================= */
function initSettingsAndDropdown() {
    if (headerSettingsBtn && settingsDropdown) {
        headerSettingsBtn.addEventListener("click", (e) => {
            e.stopPropagation();
            settingsDropdown.style.display = settingsDropdown.style.display === "none" ? "flex" : "none";
        });

        document.addEventListener("click", () => {
            settingsDropdown.style.display = "none";
        });
    }

    const openSettingsBtn = document.getElementById("open-settings-modal-btn");
    if (openSettingsBtn && settingsModal) {
        openSettingsBtn.addEventListener("click", () => {
            settingsDropdown.style.display = "none";
            settingsModal.style.display = "flex";
            loadUserSettings();
        });
    }

    if (closeSettingsModalBtn && settingsModal) {
        closeSettingsModalBtn.addEventListener("click", () => {
            settingsModal.style.display = "none";
        });
    }

    const saveSettingsBtn = document.getElementById("save-settings-btn");
    if (saveSettingsBtn) {
        saveSettingsBtn.addEventListener("click", async () => {
            const name = document.getElementById("settings-name").value.trim();
            const timezone = document.getElementById("settings-timezone").value;
            const workStartHour = parseInt(document.getElementById("settings-start-hour").value, 10);
            const workEndHour = parseInt(document.getElementById("settings-end-hour").value, 10);
            const theme = document.getElementById("settings-theme").value;
            const statusSpan = document.getElementById("settings-status");

            try {
                await fetch("/api/user/settings", {
                    method: "PUT",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ displayName: name, timezone, workStartHour, workEndHour, theme })
                });
                applyTheme(theme);
                if (name) {
                    const homeUser = document.getElementById("home-user-name");
                    if (homeUser) homeUser.textContent = name;
                }
                statusSpan.textContent = "Saved!";
                setTimeout(() => statusSpan.textContent = "", 2500);
            } catch (e) {
                statusSpan.textContent = `Error: ${e.message}`;
            }
        });
    }

    // Export buttons
    const exportHandler = async () => {
        try {
            const res = await fetch("/api/user/export");
            const data = await res.json();
            const blob = new Blob([JSON.stringify(data, null, 2)], { type: "application/json" });
            const url = URL.createObjectURL(blob);
            const a = document.createElement("a");
            a.href = url;
            a.download = `omniassist-backup-${new Date().toISOString().split("T")[0]}.json`;
            a.click();
        } catch (e) {
            alert(`Export failed: ${e.message}`);
        }
    };

    document.getElementById("quick-export-btn")?.addEventListener("click", exportHandler);
    document.getElementById("export-data-btn")?.addEventListener("click", exportHandler);
}

async function loadUserSettings() {
    try {
        const res = await fetch("/api/user/settings");
        if (!res.ok) return;
        const s = await res.json();
        const nameField = document.getElementById("settings-name");
        if (nameField) nameField.value = s.displayName || "";
        const tzField = document.getElementById("settings-timezone");
        if (tzField) tzField.value = s.timezone || "UTC";
        const startField = document.getElementById("settings-start-hour");
        if (startField) startField.value = s.workStartHour ?? 9;
        const endField = document.getElementById("settings-end-hour");
        if (endField) endField.value = s.workEndHour ?? 17;
        const themeField = document.getElementById("settings-theme");
        if (themeField) themeField.value = s.theme || "light";
    } catch (e) {
        console.error("Failed to load settings:", e);
    }
}

/* =========================================================
   AUTH & USER PROFILE
   ========================================================= */
async function initAuthAndProfile() {
    try {
        const res = await fetch("/api/me");
        if (!res.ok) return;
        const data = await res.json();

        if (data.authenticated) {
            currentUser = data;
            if (signInButton) signInButton.style.display = "none";
            if (profileCard) profileCard.style.display = "flex";

            const avatar = document.getElementById("google-avatar");
            if (avatar) avatar.src = data.picture || "/favicon.ico";
            const nameEl = document.getElementById("google-name");
            if (nameEl) nameEl.textContent = data.name || "User";
            const homeName = document.getElementById("home-user-name");
            if (homeName) homeName.textContent = data.name || "User";
        } else {
            if (signInButton) signInButton.style.display = "inline-block";
            if (profileCard) profileCard.style.display = "none";
        }
    } catch (e) {
        console.error("Auth check failed:", e);
    }
}

/* =========================================================
   CONVERSATIONS & HISTORY
   ========================================================= */
async function initConversations() {
    document.getElementById("new-chat-btn")?.addEventListener("click", () => {
        switchToView("chat");
        startNewConversation();
    });

    await loadConversationList();
}

async function loadConversationList() {
    const listContainer = document.getElementById("conversation-list");
    if (!listContainer) return;

    try {
        const res = await fetch("/api/chat/conversations");
        if (!res.ok) return;
        const conversations = await res.json();

        listContainer.innerHTML = "";
        conversations.forEach(c => {
            const btn = document.createElement("button");
            btn.className = `conv-item ${c.id === conversationId ? "active" : ""}`;
            btn.textContent = c.title || "Conversation";
            btn.addEventListener("click", () => {
                switchToView("chat");
                loadConversation(c.id);
            });
            listContainer.appendChild(btn);
        });
    } catch (e) {
        console.error("Failed to load conversations:", e);
    }
}

async function startNewConversation() {
    conversationId = null;
    messages.innerHTML = `
        <div class="assistant-message">
            <div class="msg-avatar">
                <svg viewBox="0 0 24 24" width="16" height="16" stroke="currentColor" stroke-width="2" fill="none">
                    <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"></polygon>
                </svg>
            </div>
            <div class="msg-body">
                <strong>OmniAssist</strong>
                <p>Started a new session. How can I assist you with your schedule, tasks, emails, or knowledge documents?</p>
            </div>
        </div>
    `;
    await loadConversationList();
}

async function loadConversation(id) {
    conversationId = id;
    try {
        const res = await fetch(`/api/chat/conversations/${id}/messages`);
        if (!res.ok) return;
        const history = await res.json();

        messages.innerHTML = "";
        history.forEach(m => {
            if (m.role === "USER") {
                addUserMessage(m.content);
            } else if (m.role === "ASSISTANT") {
                addAssistantMessage(m.content);
            }
        });
        await loadConversationList();
        scrollToBottom();
    } catch (e) {
        console.error("Failed to load conversation history:", e);
    }
}

/* =========================================================
   CHAT FORM & SUBMISSION
   ========================================================= */
if (form) {
    form.addEventListener("submit", async function (event) {
        event.preventDefault();
        const text = input.value.trim();
        if (!text) return;

        addUserMessage(text);
        input.value = "";
        input.disabled = true;

        const loading = addAssistantMessage("Thinking...");

        try {
            if (!conversationId) {
                const convRes = await fetch("/api/chat/conversations", { method: "POST" });
                if (convRes.ok) {
                    const conv = await convRes.json();
                    conversationId = conv.id;
                    loadConversationList();
                }
            }

            const res = await fetch("/api/chat", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ conversationId, message: text })
            });

            if (!res.ok) {
                throw new Error(`Chat error: ${res.status}`);
            }

            const data = await res.json();
            loading.querySelector("p").textContent = data.message || "";
            speakText(data.message);

            if (data.draft) {
                addEmailDraftCard(data.draft);
            }

            if (data.meetingDraft) {
                addMeetingDraftCard(data.meetingDraft);
            }

            if (data.suggestionChips && data.suggestionChips.length > 0) {
                renderSuggestionChips(data.suggestionChips);
            }

        } catch (err) {
            console.error(err);
            loading.querySelector("p").textContent = "Sorry, I encountered an issue processing your request.";
        } finally {
            input.disabled = false;
            input.focus();
            scrollToBottom();
        }
    });
}

function addUserMessage(text) {
    const el = document.createElement("div");
    el.className = "user-message";
    el.innerHTML = `
        <div class="msg-avatar">You</div>
        <div class="msg-body">
            <strong>You</strong>
            <p>${escapeHtml(text)}</p>
        </div>
    `;
    messages.appendChild(el);
    scrollToBottom();
}

function addAssistantMessage(text) {
    const el = document.createElement("div");
    el.className = "assistant-message";
    el.innerHTML = `
        <div class="msg-avatar">
            <svg viewBox="0 0 24 24" width="16" height="16" stroke="currentColor" stroke-width="2" fill="none">
                <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"></polygon>
            </svg>
        </div>
        <div class="msg-body">
            <strong>OmniAssist</strong>
            <p>${escapeHtml(text)}</p>
        </div>
    `;
    messages.appendChild(el);
    scrollToBottom();
    return el;
}

function scrollToBottom() {
    if (messages) messages.scrollTop = messages.scrollHeight;
}

function escapeHtml(text) {
    const div = document.createElement("div");
    div.innerText = text || "";
    return div.innerHTML;
}

/* Suggestion Chips */
function initSuggestionChips() {
    document.querySelectorAll(".chip").forEach(chip => {
        chip.addEventListener("click", () => {
            input.value = chip.getAttribute("data-query") || chip.textContent;
            form.dispatchEvent(new Event("submit"));
        });
    });
}

function renderSuggestionChips(chips) {
    if (!suggestionChipsContainer) return;
    suggestionChipsContainer.innerHTML = "";
    chips.forEach(q => {
        const btn = document.createElement("button");
        btn.className = "chip";
        btn.textContent = q;
        btn.addEventListener("click", () => {
            input.value = q;
            form.dispatchEvent(new Event("submit"));
        });
        suggestionChipsContainer.appendChild(btn);
    });
}

/* =========================================================
   EMAIL & MEETING DRAFT CARDS
   ========================================================= */
function addEmailDraftCard(draft) {
    const card = document.createElement("div");
    card.className = "email-draft-card";
    card.id = `draft-${draft.id}`;

    card.innerHTML = `
        <div class="draft-header">
            <span class="draft-badge">Email Draft Pending Review</span>
            <span class="sub-text">Confirm before sending</span>
        </div>
        <div class="draft-field">
            <label>To</label>
            <input type="email" class="text-input draft-to" value="${escapeHtml(draft.to || '')}">
        </div>
        <div class="draft-field">
            <label>Subject</label>
            <input type="text" class="text-input draft-subject" value="${escapeHtml(draft.subject || '')}">
        </div>
        <div class="draft-field">
            <label>Body</label>
            <textarea class="text-input draft-body" rows="4">${escapeHtml(draft.body || '')}</textarea>
        </div>
        <div class="draft-actions">
            <button class="primary-btn btn-send-draft">Send Email</button>
            <button class="secondary-btn btn-cancel-draft">Cancel</button>
        </div>
        <div class="draft-status" style="margin-top: 10px; font-size: 12px;"></div>
    `;

    const statusDiv = card.querySelector(".draft-status");

    card.querySelector(".btn-send-draft").addEventListener("click", async () => {
        statusDiv.innerHTML = "<em>Sending email through Gmail...</em>";
        try {
            const to = card.querySelector(".draft-to")?.value;
            const subject = card.querySelector(".draft-subject")?.value;
            const body = card.querySelector(".draft-body")?.value;

            // Sync user edits to the draft prior to sending
            await fetch(`/api/email/draft/${draft.id}`, {
                method: "PUT",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ to, subject, body })
            }).catch(() => {});

            const res = await fetch(`/api/email/draft/${draft.id}/send`, { method: "POST" });
            if (!res.ok) {
                const errText = await res.text();
                let errMsg = errText;
                try {
                    const parsed = JSON.parse(errText);
                    errMsg = parsed.message || parsed.error || errText;
                } catch (_) {}
                throw new Error(errMsg);
            }
            statusDiv.innerHTML = "<strong style='color: var(--success);'>Email successfully sent!</strong>";
            card.querySelectorAll("button, input, textarea").forEach(e => e.disabled = true);
        } catch (e) {
            statusDiv.innerHTML = `<span style='color: var(--danger);'>Failed to send: ${e.message}</span>`;
        }
    });

    card.querySelector(".btn-cancel-draft").addEventListener("click", async () => {
        await fetch(`/api/email/draft/${draft.id}`, { method: "DELETE" }).catch(() => {});
        card.remove();
    });

    messages.appendChild(card);
    scrollToBottom();
}

function addMeetingDraftCard(draft) {
    const card = document.createElement("div");
    card.className = "meeting-draft-card";
    card.id = `meeting-draft-${draft.id}`;

    const attendeesStr = (draft.attendees || []).join(", ");

    card.innerHTML = `
        <div class="draft-header">
            <span class="draft-badge">Meeting Draft Pending Review</span>
            <span class="sub-text">Confirm before scheduling</span>
        </div>
        <div class="draft-field">
            <label>Title</label>
            <input type="text" class="text-input draft-title" value="${escapeHtml(draft.title || '')}">
        </div>
        <div class="form-row">
            <div class="form-group">
                <label>Start Date/Time</label>
                <input type="text" class="text-input draft-start" value="${escapeHtml(draft.startDateTime || '')}">
            </div>
            <div class="form-group">
                <label>End Date/Time</label>
                <input type="text" class="text-input draft-end" value="${escapeHtml(draft.endDateTime || '')}">
            </div>
        </div>
        <div class="draft-field">
            <label>Attendees</label>
            <input type="text" class="text-input draft-attendees" value="${escapeHtml(attendeesStr)}">
        </div>
        <div class="draft-field">
            <label>Notes / Agenda</label>
            <textarea class="text-input draft-desc" rows="3">${escapeHtml(draft.description || '')}</textarea>
        </div>
        <div class="draft-actions">
            <button class="primary-btn btn-schedule-meeting">Confirm & Schedule</button>
            <button class="secondary-btn btn-cancel-meeting">Cancel</button>
        </div>
        <div class="draft-status" style="margin-top: 10px; font-size: 12px;"></div>
    `;

    const statusDiv = card.querySelector(".draft-status");

    card.querySelector(".btn-schedule-meeting").addEventListener("click", async () => {
        statusDiv.innerHTML = "<em>Adding event to Google Calendar...</em>";
        try {
            const title = card.querySelector(".draft-title")?.value;
            const location = card.querySelector(".draft-location")?.value;
            const startDateTime = card.querySelector(".draft-start")?.value;
            const endDateTime = card.querySelector(".draft-end")?.value;
            const attendeesStr = card.querySelector(".draft-attendees")?.value || "";
            const attendees = attendeesStr.split(",").map(s => s.trim()).filter(Boolean);
            const description = card.querySelector(".draft-desc")?.value;

            // Sync user edits to the meeting draft prior to scheduling
            await fetch(`/api/calendar/draft/${draft.id}`, {
                method: "PUT",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ title, location, startDateTime, endDateTime, attendees, description })
            }).catch(() => {});

            const res = await fetch(`/api/calendar/draft/${draft.id}/schedule`, { method: "POST" });
            if (!res.ok) {
                const errText = await res.text();
                let errMsg = errText;
                try {
                    const parsed = JSON.parse(errText);
                    errMsg = parsed.message || parsed.error || errText;
                } catch (_) {}
                throw new Error(errMsg);
            }
            const data = await res.json();
            statusDiv.innerHTML = `<strong style='color: var(--success);'>Meeting scheduled! ${data.htmlLink ? `<a href="${data.htmlLink}" target="_blank" style="color:var(--accent);">Open in Calendar</a>` : ''}</strong>`;
            card.querySelectorAll("button, input, textarea").forEach(e => e.disabled = true);
        } catch (e) {
            statusDiv.innerHTML = `<span style='color: var(--danger);'>Failed to schedule: ${e.message}</span>`;
        }
    });

    card.querySelector(".btn-cancel-meeting").addEventListener("click", async () => {
        await fetch(`/api/calendar/draft/${draft.id}`, { method: "DELETE" }).catch(() => {});
        card.remove();
    });

    messages.appendChild(card);
    scrollToBottom();
}

/* =========================================================
   MORNING EXECUTIVE BRIEFING
   ========================================================= */
function initBriefingView() {
    const triggerBtn = document.getElementById("briefing-trigger-btn");
    const refreshBtn = document.getElementById("briefing-refresh-btn");
    const closeBtn = document.getElementById("briefing-close-btn");
    const listenBtn = document.getElementById("briefing-listen-btn");
    const briefingCard = document.getElementById("briefing-card");

    if (triggerBtn) {
        triggerBtn.addEventListener("click", () => {
            switchToView("chat");
            briefingCard.style.display = briefingCard.style.display === "none" ? "block" : "none";
            if (briefingCard.style.display === "block" && !currentBriefingRawText) {
                loadMorningBriefing();
            }
        });
    }

    if (refreshBtn) {
        refreshBtn.addEventListener("click", () => loadMorningBriefing());
    }

    if (closeBtn) {
        closeBtn.addEventListener("click", () => {
            briefingCard.style.display = "none";
            if (window.speechSynthesis) window.speechSynthesis.cancel();
        });
    }

    if (listenBtn) {
        listenBtn.addEventListener("click", () => {
            if (!currentBriefingRawText) return;
            if (window.speechSynthesis && window.speechSynthesis.speaking) {
                window.speechSynthesis.cancel();
                listenBtn.querySelector("span").textContent = "Listen";
            } else {
                listenBtn.querySelector("span").textContent = "Stop";
                const utterance = new SpeechSynthesisUtterance(currentBriefingRawText.replace(/[#*`_~]/g, ""));
                utterance.onend = () => listenBtn.querySelector("span").textContent = "Listen";
                utterance.onerror = () => listenBtn.querySelector("span").textContent = "Listen";
                window.speechSynthesis.speak(utterance);
            }
        });
    }
}

async function loadMorningBriefing() {
    const briefingCard = document.getElementById("briefing-card");
    const content = document.getElementById("briefing-content");
    const dateLabel = document.getElementById("briefing-date");
    if (!briefingCard || !content) return;

    briefingCard.style.display = "block";
    content.innerHTML = "<p class='loading-state'>Synthesizing executive briefing with Gemini...</p>";

    try {
        const res = await fetch("/api/briefing/today");
        if (!res.ok) throw new Error("Could not load briefing");
        const data = await res.json();

        currentBriefingRawText = data.executiveSummary || "";
        if (dateLabel && data.date) dateLabel.textContent = `Date: ${data.date}`;

        let html = `<div style="white-space: pre-wrap; margin-bottom: 12px;">${escapeHtml(data.executiveSummary || "")}</div>`;

        if (data.urgentItems && data.urgentItems.length > 0) {
            html += `<h4 style="margin: 8px 0 4px 0; color:var(--danger);">Urgent Attention Items</h4><ul>`;
            data.urgentItems.forEach(u => html += `<li>${escapeHtml(u)}</li>`);
            html += `</ul>`;
        }

        if (data.todayEvents && data.todayEvents.length > 0) {
            html += `<h4 style="margin: 8px 0 4px 0; color:var(--accent);">Today's Agenda (${data.todayEvents.length})</h4><ul>`;
            data.todayEvents.slice(0, 5).forEach(e => html += `<li><strong>${escapeHtml(e.start || '')}:</strong> ${escapeHtml(e.summary || 'Meeting')}</li>`);
            html += `</ul>`;
        }

        if (data.pendingTasks && data.pendingTasks.length > 0) {
            html += `<h4 style="margin: 8px 0 4px 0; color:var(--warning);">Priority Deliverables (${data.pendingTasks.length})</h4><ul>`;
            data.pendingTasks.slice(0, 5).forEach(t => html += `<li>[${escapeHtml(t.priority)}] ${escapeHtml(t.title)}</li>`);
            html += `</ul>`;
        }

        content.innerHTML = html;
    } catch (e) {
        content.innerHTML = `<p class='empty-state' style='color:var(--danger);'>Error: ${e.message}</p>`;
    }
}

/* =========================================================
   VIEW 2: PLANNER & TASKS CONTROLLER
   ========================================================= */
function initTasksView() {
    const filterTabs = document.querySelectorAll("[data-task-filter]");
    filterTabs.forEach(tab => {
        tab.addEventListener("click", () => {
            filterTabs.forEach(t => t.classList.remove("active"));
            tab.classList.add("active");
            currentTaskFilter = tab.getAttribute("data-task-filter");
            loadTasks();
        });
    });

    const addBtn = document.getElementById("add-task-modal-btn");
    const createBox = document.getElementById("task-create-box");
    const cancelBtn = document.getElementById("cancel-task-btn");
    const saveBtn = document.getElementById("save-task-btn");

    if (addBtn && createBox) {
        addBtn.addEventListener("click", () => createBox.style.display = "block");
    }
    if (cancelBtn && createBox) {
        cancelBtn.addEventListener("click", () => createBox.style.display = "none");
    }

    if (saveBtn) {
        saveBtn.addEventListener("click", async () => {
            const title = document.getElementById("task-title-input").value.trim();
            const priority = document.getElementById("task-priority-input").value;
            const due = document.getElementById("task-due-input").value;
            const category = document.getElementById("task-category-input").value.trim();
            const desc = document.getElementById("task-desc-input").value.trim();

            if (!title) return alert("Task title is required");

            await fetch("/api/tasks", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    title,
                    priority,
                    dueDate: due ? due + ":00" : null,
                    category,
                    description: desc
                })
            });

            createBox.style.display = "none";
            document.getElementById("task-title-input").value = "";
            document.getElementById("task-desc-input").value = "";
            loadTasks();
            loadHomeDashboard();
        });
    }

    document.getElementById("auto-block-tasks-btn")?.addEventListener("click", triggerAutoTimeBlocking);
}

async function triggerAutoTimeBlocking() {
    const today = new Date().toISOString().split("T")[0];
    const confirmBlock = confirm(`Auto-block dedicated focus time on your Google Calendar for today (${today}) based on your pending tasks?`);
    if (!confirmBlock) return;

    try {
        const res = await fetch("/api/calendar/auto-block", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ date: today, blockMinutes: 60 })
        });
        if (!res.ok) throw new Error(await res.text());
        const result = await res.json();
        alert(`${result.message || 'Focus blocks scheduled!'}\nCreated events: ${result.scheduledEvents ? result.scheduledEvents.length : 0}`);
        loadCalendarEvents();
        loadTasks();
        loadHomeDashboard();
    } catch (e) {
        alert(`Time-blocking error: ${e.message}`);
    }
}

async function loadTasks() {
    const container = document.getElementById("task-items-container");
    if (!container) return;
    container.innerHTML = "<p class='loading-state'>Loading tasks...</p>";

    let url = "/api/tasks";
    if (currentTaskFilter === "DAILY") {
        url = "/api/tasks/daily-planner";
    } else if (currentTaskFilter === "TODO" || currentTaskFilter === "COMPLETED") {
        url = `/api/tasks?status=${currentTaskFilter}`;
    }

    try {
        const res = await fetch(url);
        if (!res.ok) throw new Error("Failed to load tasks");
        const tasks = await res.json();

        if (tasks.length === 0) {
            container.innerHTML = "<p class='empty-state'>No tasks found. Click '+ Add Task' to create one.</p>";
            return;
        }

        container.innerHTML = "";
        tasks.forEach(t => {
            const card = document.createElement("div");
            card.className = "task-card";
            const isDone = t.status === "COMPLETED";

            card.innerHTML = `
                <div class="task-left">
                    <input type="checkbox" class="task-checkbox" ${isDone ? "checked" : ""}>
                    <div>
                        <div class="task-title ${isDone ? "completed" : ""}">${escapeHtml(t.title)}</div>
                        <div class="task-meta">
                            <span>${escapeHtml(t.category || "General")}</span>
                            ${t.dueDate ? `<span>Due: ${t.dueDate.replace("T", " ")}</span>` : ""}
                        </div>
                    </div>
                </div>
                <div class="task-right">
                    <span class="priority-badge priority-${t.priority}">${t.priority}</span>
                    <button class="text-btn btn-delete-task" style="color:var(--danger); margin-left:10px;">Delete</button>
                </div>
            `;

            card.querySelector(".task-checkbox").addEventListener("change", async () => {
                await fetch(`/api/tasks/${t.id}/toggle`, { method: "PATCH" });
                loadTasks();
                loadHomeDashboard();
            });

            card.querySelector(".btn-delete-task").addEventListener("click", async () => {
                if (confirm(`Delete task "${t.title}"?`)) {
                    await fetch(`/api/tasks/${t.id}`, { method: "DELETE" });
                    loadTasks();
                    loadHomeDashboard();
                }
            });

            container.appendChild(card);
        });
    } catch (e) {
        container.innerHTML = `<p class='empty-state' style='color:var(--danger);'>Error: ${e.message}</p>`;
    }
}

/* =========================================================
   VIEW 3: CALENDAR & FREE SLOTS CONTROLLER
   ========================================================= */
function initCalendarView() {
    document.getElementById("refresh-calendar-btn")?.addEventListener("click", loadCalendarEvents);
    document.getElementById("auto-block-cal-btn")?.addEventListener("click", triggerAutoTimeBlocking);

    const slotPicker = document.getElementById("slot-date-picker");
    if (slotPicker) slotPicker.value = new Date().toISOString().split("T")[0];

    document.getElementById("find-slots-btn")?.addEventListener("click", async () => {
        const date = slotPicker.value;
        const dur = document.getElementById("slot-duration-select").value;
        const resultContainer = document.getElementById("free-slots-result");
        resultContainer.innerHTML = "<p class='loading-state'>Calculating free gaps...</p>";

        try {
            const res = await fetch(`/api/calendar/free-slots?date=${date}&durationMinutes=${dur}`);
            if (!res.ok) throw new Error("Could not fetch slots (Google login required)");
            const slots = await res.json();

            if (slots.length === 0) {
                resultContainer.innerHTML = "<p class='empty-state'>No open slots found within working hours for this date.</p>";
                return;
            }

            resultContainer.innerHTML = "";
            slots.forEach(s => {
                const item = document.createElement("div");
                item.className = "slot-item";
                item.innerHTML = `
                    <span>${s.start} - ${s.end}</span>
                    <span style="color:var(--text-muted); float:right;">${s.durationMinutes} min free</span>
                `;
                resultContainer.appendChild(item);
            });
        } catch (e) {
            resultContainer.innerHTML = `<p class='empty-state' style='color:var(--danger);'>${e.message}</p>`;
        }
    });
}

async function loadCalendarEvents() {
    const container = document.getElementById("calendar-events-container");
    if (!container) return;
    container.innerHTML = "<p class='loading-state'>Loading Google Calendar events...</p>";

    try {
        const res = await fetch("/api/calendar/events?daysAhead=7&maxResults=15");
        if (!res.ok) throw new Error("Google login required to view calendar");
        const events = await res.json();

        if (events.length === 0) {
            container.innerHTML = "<p class='empty-state'>No upcoming events found for next 7 days.</p>";
            return;
        }

        container.innerHTML = "";
        events.forEach(e => {
            const item = document.createElement("div");
            item.className = "event-item";
            item.innerHTML = `
                <div class="event-title">${escapeHtml(e.summary || "Untitled Event")}</div>
                <div class="event-time">${e.start || ""} to ${e.end || ""}</div>
                ${e.location ? `<div class="event-time">Location: ${escapeHtml(e.location)}</div>` : ""}
            `;
            container.appendChild(item);
        });
    } catch (e) {
        container.innerHTML = `<p class='empty-state'>${e.message}. Sign in with Google above.</p>`;
    }
}

/* =========================================================
   VIEW 4: MAILBOX & TEMPLATES CONTROLLER
   ========================================================= */
function initInboxView() {
    const inboxBtn = document.getElementById("tab-inbox-btn");
    const sentBtn = document.getElementById("tab-sent-btn");
    const tmplBtn = document.getElementById("tab-templates-btn");
    const triageBtn = document.getElementById("tab-triage-btn");

    const mailContainer = document.getElementById("inbox-messages-container");
    const tmplContainer = document.getElementById("templates-container");
    const triageContainer = document.getElementById("inbox-triage-container");
    const refreshTriageBtn = document.getElementById("refresh-triage-btn");

    function resetTabs() {
        inboxBtn?.classList.remove("active");
        sentBtn?.classList.remove("active");
        tmplBtn?.classList.remove("active");
        triageBtn?.classList.remove("active");
        if (mailContainer) mailContainer.style.display = "none";
        if (tmplContainer) tmplContainer.style.display = "none";
        if (triageContainer) triageContainer.style.display = "none";
    }

    inboxBtn?.addEventListener("click", () => {
        resetTabs();
        inboxBtn.classList.add("active");
        mailContainer.style.display = "flex";
        loadInboxMessages(false);
    });

    sentBtn?.addEventListener("click", () => {
        resetTabs();
        sentBtn.classList.add("active");
        mailContainer.style.display = "flex";
        loadInboxMessages(true);
    });

    tmplBtn?.addEventListener("click", () => {
        resetTabs();
        tmplBtn.classList.add("active");
        tmplContainer.style.display = "grid";
        loadTemplates();
    });

    triageBtn?.addEventListener("click", () => {
        resetTabs();
        triageBtn.classList.add("active");
        triageContainer.style.display = "flex";
        loadEmailTriage();
    });

    refreshTriageBtn?.addEventListener("click", () => loadEmailTriage());
}

async function loadInboxMessages(isSent = false) {
    const container = document.getElementById("inbox-messages-container");
    if (!container) return;
    container.innerHTML = `<p class='loading-state'>Loading ${isSent ? 'sent' : 'inbox'} emails...</p>`;

    const url = isSent ? "/api/gmail/sent?maxResults=10" : "/api/gmail/latest";
    try {
        const res = await fetch(url);
        if (!res.ok) throw new Error("Google login required to inspect emails");
        const list = await res.json();

        if (list.length === 0) {
            container.innerHTML = `<p class='empty-state'>No ${isSent ? 'sent' : 'inbox'} messages found.</p>`;
            return;
        }

        container.innerHTML = "";
        list.forEach(m => {
            const item = document.createElement("div");
            item.className = "mail-item";
            item.innerHTML = `
                <div class="mail-header">
                    <span>${escapeHtml(m.subject || "(No Subject)")}</span>
                    <span class="mail-from">${escapeHtml(m.from || "")}</span>
                </div>
                <div class="mail-snippet">${escapeHtml(m.snippet || "")}</div>
            `;
            container.appendChild(item);
        });
    } catch (e) {
        container.innerHTML = `<p class='empty-state'>${e.message}. Sign in with Google above.</p>`;
    }
}

async function loadEmailTriage() {
    const container = document.getElementById("triage-cards-container");
    if (!container) return;
    container.innerHTML = "<p class='loading-state'>Classifying unread emails into action categories with Gemini...</p>";

    try {
        const res = await fetch("/api/gmail/triage");
        if (!res.ok) throw new Error("Could not triage emails (Google login required)");
        const data = await res.json();

        const items = data.triagedEmails || [];
        if (items.length === 0) {
            container.innerHTML = "<p class='empty-state'>Inbox Zero! No unread emails require triage right now.</p>";
            return;
        }

        const actionNeeded = items.filter(i => i.category === "ACTION_NEEDED");
        const waiting = items.filter(i => i.category === "WAITING_ON_OTHERS");
        const info = items.filter(i => i.category === "INFORMATIONAL");

        container.innerHTML = `
            <div class="triage-col">
                <div class="triage-col-header" style="color:var(--danger);">
                    <span>Action Needed</span>
                    <span>${actionNeeded.length}</span>
                </div>
                ${renderTriageList(actionNeeded)}
            </div>
            <div class="triage-col">
                <div class="triage-col-header" style="color:var(--warning);">
                    <span>Waiting on Others</span>
                    <span>${waiting.length}</span>
                </div>
                ${renderTriageList(waiting)}
            </div>
            <div class="triage-col">
                <div class="triage-col-header" style="color:var(--accent);">
                    <span>Informational</span>
                    <span>${info.length}</span>
                </div>
                ${renderTriageList(info)}
            </div>
        `;

        container.querySelectorAll(".quick-reply-btn").forEach(btn => {
            btn.addEventListener("click", () => {
                const replyText = btn.getAttribute("data-reply");
                const subject = btn.getAttribute("data-subject");
                const from = btn.getAttribute("data-from");
                switchToChatWithQuery(`Draft a polite reply to "${from}" regarding "${subject}": ${replyText}`);
            });
        });

    } catch (e) {
        container.innerHTML = `<p class='empty-state' style='color:var(--danger);'>${e.message}</p>`;
    }
}

function renderTriageList(list) {
    if (list.length === 0) return "<p class='empty-state' style='font-size:12px;'>No items in this category</p>";
    return list.map(item => `
        <div class="triage-card">
            <div class="triage-card-header">
                <div class="triage-card-subject">${escapeHtml(item.subject || '(No Subject)')}</div>
                <span class="priority-badge priority-${item.urgency || 'MEDIUM'}">${item.urgency || 'MEDIUM'}</span>
            </div>
            <div class="triage-card-from">From: ${escapeHtml(item.from || '')}</div>
            <div class="triage-reason">${escapeHtml(item.reason || '')}</div>
            ${item.suggestedQuickReplies && item.suggestedQuickReplies.length > 0 ? `
                <div class="quick-reply-row">
                    ${item.suggestedQuickReplies.map(r => `
                        <button class="quick-reply-btn" data-reply="${escapeHtml(r)}" data-subject="${escapeHtml(item.subject || '')}" data-from="${escapeHtml(item.from || '')}">
                            Reply: ${escapeHtml(r)}
                        </button>
                    `).join('')}
                </div>
            ` : ''}
        </div>
    `).join('');
}

async function loadTemplates() {
    const container = document.getElementById("templates-container");
    if (!container) return;
    container.innerHTML = "<p class='loading-state'>Loading email templates...</p>";

    try {
        const res = await fetch("/api/gmail/templates");
        if (!res.ok) throw new Error("Failed to load templates");
        const list = await res.json();

        container.innerHTML = "";
        list.forEach(t => {
            const card = document.createElement("div");
            card.className = "card";
            card.innerHTML = `
                <h4>${escapeHtml(t.name)}</h4>
                <div style="font-size:12px; color:var(--text-muted); margin-bottom:8px;">Subject: ${escapeHtml(t.subject)}</div>
                <p style="font-size:12.5px; white-space:pre-wrap; line-height:1.5;">${escapeHtml(t.body)}</p>
                <button class="primary-btn btn-use-template" style="margin-top:10px;">Use in Chat</button>
            `;
            card.querySelector(".btn-use-template").addEventListener("click", () => {
                switchToChatWithQuery(`Draft an email using template '${t.name}' with subject '${t.subject}'`);
            });
            container.appendChild(card);
        });
    } catch (e) {
        container.innerHTML = `<p class='empty-state'>${e.message}</p>`;
    }
}

/* =========================================================
   VIEW 5: KNOWLEDGE & NOTES CONTROLLER
   ========================================================= */
function initKnowledgeView() {
    const createBox = document.getElementById("note-create-box");
    const minutesBox = document.getElementById("meeting-minutes-box");

    document.getElementById("create-note-toggle-btn")?.addEventListener("click", () => {
        createBox.style.display = createBox.style.display === "none" ? "block" : "none";
        if (minutesBox) minutesBox.style.display = "none";
    });

    document.getElementById("cancel-note-btn")?.addEventListener("click", () => {
        createBox.style.display = "none";
    });

    // Meeting minutes
    const minutesToggleBtn = document.getElementById("process-minutes-toggle-btn");
    const cancelMinutesBtn = document.getElementById("cancel-minutes-btn");
    const processMinutesBtn = document.getElementById("process-minutes-btn");
    const minutesResultBox = document.getElementById("minutes-result-box");

    if (minutesToggleBtn && minutesBox) {
        minutesToggleBtn.addEventListener("click", () => {
            minutesBox.style.display = minutesBox.style.display === "none" ? "block" : "none";
            if (createBox) createBox.style.display = "none";
        });
    }

    if (cancelMinutesBtn && minutesBox) {
        cancelMinutesBtn.addEventListener("click", () => {
            minutesBox.style.display = "none";
        });
    }

    if (processMinutesBtn && minutesBox) {
        processMinutesBtn.addEventListener("click", async () => {
            const title = document.getElementById("meeting-title-input").value.trim();
            const attendeesStr = document.getElementById("meeting-attendees-input").value.trim();
            const notes = document.getElementById("meeting-notes-input").value.trim();

            if (!notes) return alert("Please provide meeting notes or transcript to analyze.");

            processMinutesBtn.disabled = true;
            processMinutesBtn.textContent = "Processing with Gemini...";
            minutesResultBox.style.display = "block";
            minutesResultBox.innerHTML = "<p class='loading-state'>Extracting decisions, tasks, and follow-up draft...</p>";

            try {
                const attendees = attendeesStr ? attendeesStr.split(",").map(s => s.trim()).filter(Boolean) : [];
                const res = await fetch("/api/meeting/process-notes", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ title, attendees, rawNotes: notes })
                });

                if (!res.ok) throw new Error(await res.text());
                const data = await res.json();

                let html = `
                    <div style="background:var(--bg-elevated); padding:12px; border-radius:var(--radius-md); border:1px solid var(--border-color);">
                        <strong style="color:var(--success);">Meeting Processed Successfully!</strong>
                        <p style="margin:8px 0; font-size:12.5px;">${escapeHtml(data.executiveSummary || '')}</p>
                        <div style="font-size:12px; margin-top:6px;">
                            <strong>Created Tasks (${(data.createdTasks || []).length}):</strong>
                            <ul style="margin:4px 0 8px 16px;">
                                ${(data.createdTasks || []).map(t => `<li>[${escapeHtml(t.priority)}] ${escapeHtml(t.title)}</li>`).join('')}
                            </ul>
                        </div>
                        ${data.draftCreated ? `<div style="font-size:12px; color:var(--accent);">Follow-up email draft created with subject: "${escapeHtml(data.emailSubject || '')}"</div>` : ''}
                    </div>
                `;
                minutesResultBox.innerHTML = html;
                loadKnowledgeNotes();
                loadTasks();
                loadHomeDashboard();
            } catch (e) {
                minutesResultBox.innerHTML = `<p style="color:var(--danger); font-size:12.5px;">Error: ${e.message}</p>`;
            } finally {
                processMinutesBtn.disabled = false;
                processMinutesBtn.textContent = "Extract Deliverables & Draft Email";
            }
        });
    }

    // Document Q&A modal
    const docQaModal = document.getElementById("doc-qa-modal");
    const closeDocQaBtn = document.getElementById("close-doc-qa-btn");
    const docQaSubmitBtn = document.getElementById("doc-qa-submit-btn");
    const docQaInput = document.getElementById("doc-qa-question-input");
    const docQaAnswerBox = document.getElementById("doc-qa-answer-container");
    const docQaAnswerText = document.getElementById("doc-qa-answer-text");
    const docQaCitations = document.getElementById("doc-qa-citations-list");

    if (closeDocQaBtn && docQaModal) {
        closeDocQaBtn.addEventListener("click", () => {
            docQaModal.style.display = "none";
            currentQaDocId = null;
        });
    }

    if (docQaSubmitBtn && docQaInput) {
        docQaSubmitBtn.addEventListener("click", async () => {
            const query = docQaInput.value.trim();
            if (!query || !currentQaDocId) return;

            docQaSubmitBtn.disabled = true;
            docQaSubmitBtn.textContent = "Searching...";
            docQaAnswerBox.style.display = "block";
            docQaAnswerText.textContent = "Retrieving relevant excerpts and generating answer...";
            docQaCitations.innerHTML = "";

            try {
                const res = await fetch(`/api/knowledge/${currentQaDocId}/qa`, {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ query })
                });

                if (!res.ok) throw new Error(await res.text());
                const data = await res.json();

                docQaAnswerText.textContent = data.answer || "No response provided.";
                if (data.citations && data.citations.length > 0) {
                    docQaCitations.innerHTML = data.citations.map(c => `
                        <div class="citation-item">${escapeHtml(c.section || '')}: ${escapeHtml(c.excerpt || '')}</div>
                    `).join("");
                } else {
                    docQaCitations.innerHTML = "<small class='sub-text'>Direct document synthesis.</small>";
                }
            } catch (e) {
                docQaAnswerText.textContent = `Error: ${e.message}`;
            } finally {
                docQaSubmitBtn.disabled = false;
                docQaSubmitBtn.textContent = "Ask AI";
            }
        });
    }

    document.getElementById("save-note-btn")?.addEventListener("click", async () => {
        const title = document.getElementById("note-title-input").value.trim();
        const content = document.getElementById("note-content-input").value.trim();
        const tags = document.getElementById("note-tags-input").value.trim();

        if (!title || !content) return alert("Title and content are required.");

        await fetch("/api/knowledge/notes", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ title, content, tags })
        });

        createBox.style.display = "none";
        document.getElementById("note-title-input").value = "";
        document.getElementById("note-content-input").value = "";
        loadKnowledgeNotes();
    });

    const fileInput = document.getElementById("file-upload-input");
    fileInput?.addEventListener("change", async () => {
        if (!fileInput.files || fileInput.files.length === 0) return;
        const file = fileInput.files[0];
        const formData = new FormData();
        formData.append("file", file);

        try {
            const res = await fetch("/api/knowledge/upload", { method: "POST", body: formData });
            if (!res.ok) throw new Error(await res.text());
            alert(`File "${file.name}" indexed into knowledge base!`);
            loadKnowledgeNotes();
        } catch (e) {
            alert(`Upload error: ${e.message}`);
        }
    });

    document.getElementById("knowledge-search-btn")?.addEventListener("click", () => {
        const q = document.getElementById("knowledge-search-input").value.trim();
        loadKnowledgeNotes(q);
    });
}

async function loadKnowledgeNotes(searchQuery = "") {
    const container = document.getElementById("notes-grid-container");
    if (!container) return;
    container.innerHTML = "<p class='loading-state'>Loading knowledge base...</p>";

    const url = searchQuery ? `/api/knowledge/search?q=${encodeURIComponent(searchQuery)}` : "/api/knowledge";
    try {
        const res = await fetch(url);
        if (!res.ok) throw new Error("Failed to load notes");
        const list = await res.json();

        if (list.length === 0) {
            container.innerHTML = "<p class='empty-state'>No notes found. Create a note or upload a file above.</p>";
            return;
        }

        container.innerHTML = "";
        list.forEach(n => {
            const card = document.createElement("div");
            card.className = "note-card";
            card.innerHTML = `
                <div>
                    <div class="note-title">${escapeHtml(n.title)}</div>
                    <div class="note-preview">${escapeHtml(n.summary || n.content)}</div>
                </div>
                <div style="display:flex; justify-content:space-between; align-items:center; margin-top:10px;">
                    <span class="note-tag">${escapeHtml(n.sourceType)} ${n.tags ? `• ${escapeHtml(n.tags)}` : ''}</span>
                    <div style="display:flex; align-items:center; gap:8px;">
                        <button class="secondary-btn btn-qa-doc" style="padding:3px 10px; font-size:12px;">Ask AI</button>
                        <button class="text-btn btn-del-note" style="color:var(--danger);">Delete</button>
                    </div>
                </div>
            `;

            card.querySelector(".btn-qa-doc").addEventListener("click", () => {
                currentQaDocId = n.id;
                const modal = document.getElementById("doc-qa-modal");
                const modalTitle = document.getElementById("doc-qa-title");
                const answerBox = document.getElementById("doc-qa-answer-container");
                const questionInput = document.getElementById("doc-qa-question-input");

                if (modalTitle) modalTitle.textContent = `Q&A: ${n.title}`;
                if (answerBox) answerBox.style.display = "none";
                if (questionInput) {
                    questionInput.value = "";
                    questionInput.focus();
                }
                if (modal) modal.style.display = "flex";
            });

            card.querySelector(".btn-del-note").addEventListener("click", async () => {
                if (confirm(`Delete note "${n.title}"?`)) {
                    await fetch(`/api/knowledge/${n.id}`, { method: "DELETE" });
                    loadKnowledgeNotes(searchQuery);
                }
            });
            container.appendChild(card);
        });
    } catch (e) {
        container.innerHTML = `<p class='empty-state' style='color:var(--danger);'>Error: ${e.message}</p>`;
    }
}

/* =========================================================
   NOTIFICATIONS
   ========================================================= */
function initNotifications() {
    const bellBtn = document.getElementById("notification-bell-btn");
    const drawer = document.getElementById("notification-drawer");
    const markAllBtn = document.getElementById("mark-all-read-btn");

    if (bellBtn && drawer) {
        bellBtn.addEventListener("click", (e) => {
            e.stopPropagation();
            drawer.style.display = drawer.style.display === "none" ? "flex" : "none";
            loadNotifications();
        });

        document.addEventListener("click", () => {
            drawer.style.display = "none";
        });
    }

    if (markAllBtn) {
        markAllBtn.addEventListener("click", async () => {
            await fetch("/api/notifications/read-all", { method: "POST" });
            loadNotifications();
        });
    }

    loadNotifications();
}

async function loadNotifications() {
    const badge = document.getElementById("notification-badge");
    const container = document.getElementById("notification-items");
    if (!badge || !container) return;

    try {
        const res = await fetch("/api/notifications");
        if (!res.ok) return;
        const notifs = await res.json();

        const unreadCount = notifs.filter(n => !n.read).length;
        if (unreadCount > 0) {
            badge.textContent = unreadCount;
            badge.style.display = "inline-block";
        } else {
            badge.style.display = "none";
        }

        if (notifs.length === 0) {
            container.innerHTML = "<p class='empty-state'>No notifications</p>";
            return;
        }

        container.innerHTML = "";
        notifs.slice(0, 15).forEach(n => {
            const card = document.createElement("div");
            card.className = `notif-card ${!n.read ? 'unread' : ''}`;
            card.innerHTML = `
                <div class="notif-title">${escapeHtml(n.title)}</div>
                <div style="font-size:12px; color:var(--text-secondary);">${escapeHtml(n.message || '')}</div>
                <div class="notif-time">${n.createdAt ? n.createdAt.replace("T", " ").substring(0, 16) : ''}</div>
            `;
            container.appendChild(card);
        });
    } catch (e) {
        console.error("Notification load error:", e);
    }
}