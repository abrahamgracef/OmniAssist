const form = document.getElementById("chat-form");
const input = document.getElementById("message-input");
const messages = document.getElementById("messages");

const signInButton = document.getElementById("google-signin");
const profile = document.getElementById("google-profile");
const profileMenu = document.getElementById("profile-menu");

let conversationId = null;


/* ========================================
   CHAT
   ======================================== */

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
            const conversationResponse =
                await fetch("/api/chat/conversations", {
                    method: "POST"
                });

            if (!conversationResponse.ok) {
                throw new Error(
                    `Unable to create conversation: ${conversationResponse.status}`
                );
            }

            const conversation =
                await conversationResponse.json();

            conversationId = conversation.id;
        }

        const response = await fetch("/api/chat", {
            method: "POST",

            headers: {
                "Content-Type": "application/json"
            },

            body: JSON.stringify({
                conversationId: conversationId,
                message: text
            })
        });

        if (!response.ok) {
            const errorBody = await response.text();

            console.error(
                "Chat request failed:",
                response.status,
                errorBody
            );

            throw new Error(
                `Chat request failed: ${response.status}`
            );
        }

        const data = await response.json();

        loading.querySelector("p").textContent =
            data.message ?? "";

        if (data.draft) {
            addEmailDraftCard(data.draft);
        }

    } catch (error) {
        console.error(error);

        loading.querySelector("p").textContent =
            "Sorry, I couldn't process that request.";

    } finally {
        input.disabled = false;
        input.focus();
    }

    scrollToBottom();
});


/* ========================================
   CHAT MESSAGE UI
   ======================================== */

function addUserMessage(text) {
    const element = document.createElement("div");
    element.className = "user-message";

    const content = document.createElement("div");
    content.className = "message-content";
    content.textContent = text;

    element.appendChild(content);
    messages.appendChild(element);

    scrollToBottom();
}
function addAssistantMessage(text) {
    const element = document.createElement("div");
    element.className = "assistant-message";

    const avatar = document.createElement("div");
    avatar.className = "avatar";
    avatar.textContent = "O";

    const content = document.createElement("div");
    content.className = "message-content";

    const name = document.createElement("strong");
    name.textContent = "OmniAssist";

    const paragraph = document.createElement("p");
    paragraph.textContent = text;

    content.appendChild(name);
    content.appendChild(paragraph);

    element.appendChild(avatar);
    element.appendChild(content);

    messages.appendChild(element);

    scrollToBottom();

    return element;
}


/* ========================================
   EMAIL DRAFT CARD
   ======================================== */

function addEmailDraftCard(draft) {
    let editing = false;

    const wrapper = document.createElement("div");
    wrapper.className = "email-draft-wrapper";

    const card = document.createElement("div");
    card.className = "email-draft-card";


    /* HEADER */

    const header = document.createElement("div");
    header.className = "email-draft-header";

    const title = document.createElement("strong");
    title.textContent = "Email Draft";

    const badge = document.createElement("span");
    badge.className = "draft-badge";
    badge.textContent = "Not sent";

    header.appendChild(title);
    header.appendChild(badge);


    /* TO */

    const toGroup =
        createEditableDraftField(
            "To",
            draft.to
        );


    /* SUBJECT */

    const subjectGroup =
        createEditableDraftField(
            "Subject",
            draft.subject
        );


    /* BODY */

    const bodyGroup = document.createElement("div");
    bodyGroup.className = "email-draft-field";

    const bodyLabel = document.createElement("span");
    bodyLabel.className = "email-draft-label";
    bodyLabel.textContent = "Message";

    const body = document.createElement("textarea");
    body.className = "email-draft-body draft-body-input";
    body.value = draft.body ?? "";
    body.readOnly = true;

    bodyGroup.appendChild(bodyLabel);
    bodyGroup.appendChild(body);


    /* ACTIONS */

    const actions = document.createElement("div");
    actions.className = "email-draft-actions";

    const cancelButton = document.createElement("button");
    cancelButton.type = "button";
    cancelButton.className = "draft-cancel-button";
    cancelButton.textContent = "Cancel";

    const editButton = document.createElement("button");
    editButton.type = "button";
    editButton.className = "draft-edit-button";
    editButton.textContent = "Edit";

    const sendButton = document.createElement("button");
    sendButton.type = "button";
    sendButton.className = "draft-send-button";
    sendButton.textContent = "Send Email";

    actions.appendChild(cancelButton);
    actions.appendChild(editButton);
    actions.appendChild(sendButton);


    /* BUILD */

    card.appendChild(header);
    card.appendChild(toGroup);
    card.appendChild(subjectGroup);
    card.appendChild(bodyGroup);
    card.appendChild(actions);

    wrapper.appendChild(card);

    messages.appendChild(wrapper);


    /* ========================================
       EDIT / SAVE
       ======================================== */

    editButton.addEventListener("click", async function () {
        const toInput =
            toGroup.querySelector("input");

        const subjectInput =
            subjectGroup.querySelector("input");

        // Enter edit mode
        if (!editing) {
            editing = true;

            toInput.readOnly = false;
            subjectInput.readOnly = false;
            body.readOnly = false;

            editButton.textContent = "Save";

            card.classList.add("editing");

            return;
        }

        // Save changes
        const updatedDraft = {
            to: toInput.value.trim(),
            subject: subjectInput.value.trim(),
            body: body.value.trim()
        };

        if (
            !updatedDraft.to ||
            !updatedDraft.subject ||
            !updatedDraft.body
        ) {
            addAssistantMessage(
                "To, subject and message cannot be empty."
            );

            return;
        }

        editButton.disabled = true;
        editButton.textContent = "Saving...";

        try {
            const response = await fetch(
                `/api/email/draft/${draft.id}`,
                {
                    method: "PUT",

                    headers: {
                        "Content-Type": "application/json"
                    },

                    body: JSON.stringify(updatedDraft)
                }
            );

            if (!response.ok) {
                const errorBody =
                    await response.text();

                console.error(
                    "Draft update failed:",
                    response.status,
                    errorBody
                );

                throw new Error(
                    `Draft update failed: ${response.status}`
                );
            }

            const savedDraft =
                await response.json();

            draft.to = savedDraft.to;
            draft.subject = savedDraft.subject;
            draft.body = savedDraft.body;

            toInput.value = savedDraft.to;
            subjectInput.value = savedDraft.subject;
            body.value = savedDraft.body;

            toInput.readOnly = true;
            subjectInput.readOnly = true;
            body.readOnly = true;

            editing = false;

            editButton.textContent = "Edit";

            card.classList.remove("editing");

        } catch (error) {
            console.error(error);

            editButton.textContent = "Save";

            addAssistantMessage(
                "I couldn't save the changes to the email."
            );

        } finally {
            editButton.disabled = false;
        }
    });


    /* ========================================
       CANCEL
       ======================================== */

    cancelButton.addEventListener("click", async function () {
        cancelButton.disabled = true;
        editButton.disabled = true;
        sendButton.disabled = true;

        try {
            const response = await fetch(
                `/api/email/draft/${draft.id}`,
                {
                    method: "DELETE"
                }
            );

            if (!response.ok) {
                console.warn(
                    "Draft deletion returned:",
                    response.status
                );
            }

        } catch (error) {
            console.error(
                "Unable to delete draft:",
                error
            );
        }

        wrapper.remove();

        addAssistantMessage(
            "Email draft cancelled. Nothing was sent."
        );
    });


    /* ========================================
       SEND
       ======================================== */

    sendButton.addEventListener("click", async function () {
        if (editing) {
            addAssistantMessage(
                "Save your changes before sending the email."
            );

            return;
        }

        await sendEmailDraft(
            draft,
            sendButton,
            cancelButton,
            editButton,
            badge,
            wrapper
        );
    });

    scrollToBottom();
}


/* ========================================
   EDITABLE DRAFT FIELD
   ======================================== */

function createEditableDraftField(
    labelText,
    valueText
) {
    const group = document.createElement("div");
    group.className = "email-draft-field";

    const label = document.createElement("span");
    label.className = "email-draft-label";
    label.textContent = labelText;

    const fieldInput = document.createElement("input");
    fieldInput.className = "email-draft-input";
    fieldInput.value = valueText ?? "";
    fieldInput.readOnly = true;

    group.appendChild(label);
    group.appendChild(fieldInput);

    return group;
}


/* ========================================
   SEND EMAIL
   ======================================== */

async function sendEmailDraft(
    draft,
    sendButton,
    cancelButton,
    editButton,
    badge,
    wrapper
) {
    // Prevent duplicate clicks
    sendButton.disabled = true;
    cancelButton.disabled = true;
    editButton.disabled = true;

    sendButton.textContent = "Sending...";

    try {
        const response = await fetch(
            `/api/email/draft/${draft.id}/send`,
            {
                method: "POST"
            }
        );

        if (!response.ok) {
            const errorBody =
                await response.text();

            console.error(
                "Email send failed:",
                response.status,
                errorBody
            );

            throw new Error(
                `Email send failed: ${response.status}`
            );
        }

        const result =
            await response.json();

        console.log(
            "Email sent:",
            result
        );

        badge.textContent = "Sent";
        badge.classList.add("sent");

        sendButton.textContent = "Sent";

        wrapper.classList.add("email-sent");

        // Keep everything disabled after successful send
        cancelButton.disabled = true;
        editButton.disabled = true;
        sendButton.disabled = true;

        addAssistantMessage(
            `Email sent successfully to ${draft.to}.`
        );

    } catch (error) {
        console.error(error);

        sendButton.disabled = false;
        cancelButton.disabled = false;
        editButton.disabled = false;

        sendButton.textContent = "Send Email";

        addAssistantMessage(
            "I couldn't send that email. Please try again."
        );
    }

    scrollToBottom();
}


/* ========================================
   GOOGLE ACCOUNT
   ======================================== */

async function loadGoogleAccount() {
    try {
        const response =
            await fetch("/api/me");

        if (!response.ok) {
            showSignedOut();
            return;
        }

        const user =
            await response.json();

        if (!user.authenticated) {
            showSignedOut();
            return;
        }

        showSignedIn(user);

    } catch (error) {
        console.error(
            "Failed to load Google account:",
            error
        );

        showSignedOut();
    }
}


function showSignedOut() {
    signInButton.style.display = "flex";
    profile.style.display = "none";

    profileMenu.classList.remove("open");
}


function showSignedIn(user) {
    signInButton.style.display = "none";
    profile.style.display = "flex";

    const name =
        user.name ?? "Google Account";

    const email =
        user.email ?? "";

    const picture =
        user.picture ?? "";

    document.getElementById(
        "google-name"
    ).textContent = name;

    document.getElementById(
        "google-email"
    ).textContent = email;

    document.getElementById(
        "google-avatar"
    ).src = picture;

    document.getElementById(
        "menu-google-name"
    ).textContent = name;

    document.getElementById(
        "menu-google-email"
    ).textContent = email;

    document.getElementById(
        "menu-google-avatar"
    ).src = picture;
}


/* ========================================
   PROFILE DROPDOWN
   ======================================== */

profile.addEventListener(
    "click",
    function (event) {
        event.stopPropagation();

        profileMenu.classList.toggle("open");
    }
);


profileMenu.addEventListener(
    "click",
    function (event) {
        event.stopPropagation();
    }
);


document.addEventListener(
    "click",
    function () {
        profileMenu.classList.remove("open");
    }
);


/* ========================================
   SCROLL
   ======================================== */

function scrollToBottom() {
    messages.scrollTop =
        messages.scrollHeight;
}


/* ========================================
   STARTUP
   ======================================== */

loadGoogleAccount();
input.focus();