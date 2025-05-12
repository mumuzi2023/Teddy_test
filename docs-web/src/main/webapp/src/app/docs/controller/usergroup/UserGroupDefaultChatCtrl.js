// src/main/webapp/src/app/docs/controller/UserGroupDefaultChatCtrl.js
'use strict';

angular.module('docs').controller('UserGroupDefaultChatCtrl',
    // 1. Inject $interval service
    function($scope, $rootScope, $timeout, $interval, Restangular /*, ChatService */) {

    // --- Scope Variable Definitions ---
    $scope.availableUsers = [];
    $scope.selectedUserToChatWith = null;
    $scope.messages = {};
    $scope.chatInput = { text: '' };
    $scope.currentUser = $rootScope.userInfo;
    $scope.loggedInUsername = null;
    $scope.isLoadingHistory = false;
    $scope.sendError = null;
    $scope.userSearchText = "";
    // 2. Add polling state variables
    let pollingIntervalPromise = null; // Use 'let' or 'var' for local controller variable
    let isPolling = false; // Flag to prevent overlapping polls
    const POLLING_INTERVAL_MS = 3000; // Poll every 3 seconds

    // --- User Fetching (No changes needed here) ---
    function initializeUsers() {
        if ($scope.currentUser && $scope.currentUser.username) {
            $scope.loggedInUsername = $scope.currentUser.username;
            console.log('[Init] Saved loggedInUsername:', $scope.loggedInUsername);
            Restangular.one('user/list').get({ sort_column: 1, asc: true })
                .then(function(data) { /* ... user processing ... */
                     if (data && data.users) {
                        let fetchedUsers = data.users.filter(u => u.username !== $scope.loggedInUsername);
                        fetchedUsers.forEach(user => {
                            if (user.unreadMessages === undefined) user.unreadMessages = 0;
                            if (!user.displayName) user.displayName = user.username;
                        });
                        $scope.availableUsers = fetchedUsers;
                    } else { $scope.availableUsers = []; }
                }).catch(function(error) { console.error("[Init] Error loading users:", error); $scope.availableUsers = []; });
        } else { /* ... handle no user info ... */
             console.warn("[Init] Current user info not available.");
             $scope.loggedInUsername = null; $scope.availableUsers = [];
        }
    }

    // --- Load Chat History via API (No changes needed here) ---
    function loadChatHistory(partnerUsername) {
        if (!$scope.loggedInUsername) { /* ... error ... */ return; }
        if (!partnerUsername) { /* ... error ... */ return; }
        console.log(`[History] Loading chat history between ${$scope.loggedInUsername} and ${partnerUsername}`);
        $scope.isLoadingHistory = true;
        $scope.messages[partnerUsername] = [];
        Restangular.one('chat/conversation', $scope.loggedInUsername).one(partnerUsername).get({ sort: 'asc' })
            .then(function(response) { /* ... message processing ... */
                if (response && response.messages) {
                    $scope.messages[partnerUsername] = response.messages.map(msg => {
                        msg.timestamp = new Date(msg.timestamp); msg.content = msg.content || "";
                        msg.text = msg.content; msg.sender = msg.senderUsername; return msg;
                    });
                    console.log(`[History] Loaded ${response.messages.length} messages for ${partnerUsername}`);
                } else { /* ... handle no messages ... */
                    $scope.messages[partnerUsername] = []; console.log(`[History] No message history found for ${partnerUsername}`);
                }
                $timeout(scrollToBottom, 50);
            })
            .catch(function(error) { /* ... error handling ... */
                 console.error(`[History] Error loading chat history with ${partnerUsername}:`, error);
                 $scope.messages[partnerUsername] = [{ id: 'error_load_' + Date.now(), sender: 'System', senderUsername: 'System', text: 'Failed to load history.', timestamp: new Date(), isError: true }];
            })
            .finally(function() { $scope.isLoadingHistory = false; });
    }

    // --- Polling Function ---
    function pollNewMessages() {
        if (isPolling || !$scope.selectedUserToChatWith || !$scope.loggedInUsername) {
            // Don't poll if already polling, no user selected, or not logged in
            return;
        }

        isPolling = true;
        const partnerUsername = $scope.selectedUserToChatWith.username;
        // console.log(`[Polling] Checking for new messages with ${partnerUsername}`); // Optional: verbose log

        // Fetch the *entire* history (inefficient workaround)
        Restangular.one('chat/conversation', $scope.loggedInUsername).one(partnerUsername).get({ sort: 'asc' })
            .then(function(response) {
                if (response && response.messages && $scope.selectedUserToChatWith && $scope.selectedUserToChatWith.username === partnerUsername) { // Check if user is still selected
                    const fetchedMessages = response.messages.map(msg => {
                        msg.timestamp = new Date(msg.timestamp);
                        msg.content = msg.content || "";
                        msg.text = msg.content;
                        msg.sender = msg.senderUsername;
                        return msg;
                    });

                    const currentMessages = $scope.messages[partnerUsername] || [];
                    const currentMessageIds = new Set(currentMessages.map(m => m.id));

                    // Filter out only new messages (not temporary ones, not errors, not already present)
                    const newMessages = fetchedMessages.filter(msg =>
                        msg.id && // Ensure message has an ID
                        !msg.id.startsWith('temp_') &&
                        !msg.id.startsWith('error_') &&
                        !currentMessageIds.has(msg.id)
                    );

                    if (newMessages.length > 0) {
                        console.log(`[Polling] Found ${newMessages.length} new message(s)`);

                        // Check if user is scrolled near the bottom before adding new messages
                        const chatArea = document.getElementById('chatMessagesArea');
                        let shouldScroll = true; // Default to true
                        if (chatArea) {
                            const threshold = 50; // Pixels from bottom
                            shouldScroll = chatArea.scrollHeight - chatArea.scrollTop <= chatArea.clientHeight + threshold;
                        }

                        // Append new messages using $applyAsync or $timeout to ensure digest cycle
                        $timeout(function() {
                             // Use Array.prototype.push.apply for potentially multiple messages
                             Array.prototype.push.apply($scope.messages[partnerUsername], newMessages);

                             // Scroll only if user was already near the bottom
                             if (shouldScroll) {
                                 scrollToBottom();
                             }
                        }, 0);
                    }
                }
            })
            .catch(function(error) {
                // Log polling errors but don't necessarily stop polling unless it's severe
                console.error(`[Polling] Error fetching messages with ${partnerUsername}:`, error);
            })
            .finally(function() {
                isPolling = false; // Allow next poll attempt
            });
    }

    // --- Start/Stop Polling Logic ---
    function startPolling() {
        stopPolling(); // Stop any previous polling first
        if ($scope.selectedUserToChatWith) {
             console.log(`[Polling] Starting polling for ${$scope.selectedUserToChatWith.username}`);
             // Call immediately first, then start interval
             pollNewMessages();
             pollingIntervalPromise = $interval(pollNewMessages, POLLING_INTERVAL_MS);
        }
    }

    function stopPolling() {
        if (pollingIntervalPromise) {
            console.log("[Polling] Stopping polling");
            $interval.cancel(pollingIntervalPromise);
            pollingIntervalPromise = null;
            isPolling = false; // Reset flag
        }
    }

    // --- Select User to Chat With (Modified to handle polling) ---
    $scope.selectUserToChat = function(user) {
        if ($scope.selectedUserToChatWith && $scope.selectedUserToChatWith.username === user.username) { return; }

        console.log(`[Select] Selecting user: ${user.username}`);
        stopPolling(); // Stop polling for the previous user
        $scope.sendError = null;
        $scope.chatInput.text = '';
        $scope.selectedUserToChatWith = user;

        if (user) {
            user.unreadMessages = 0;
            loadChatHistory(user.username); // Load initial history
            startPolling(); // Start polling for the newly selected user
        }
    };

    // --- Send Message via API (No changes needed for polling, just ensure $timeout is present) ---
    $scope.sendMessage = function() {
        if (!$scope.chatInput.text.trim() || !$scope.selectedUserToChatWith || !$scope.loggedInUsername) { /* ... checks ... */ return; }
        $scope.sendError = null;
        const partnerUsername = $scope.selectedUserToChatWith.username;
        const senderUsername = $scope.loggedInUsername;
        const messageContent = $scope.chatInput.text;

        // Optimistic Update
        const optimisticMessage = { /* ... */
            id: 'temp_' + Date.now() + Math.random(), sender: senderUsername, senderUsername: senderUsername,
            senderDisplayName: ($scope.currentUser ? $scope.currentUser.displayName : null) || senderUsername,
            recipientUsername: partnerUsername, content: messageContent, text: messageContent,
            timestamp: new Date(), isSending: true
         };
        if (!$scope.messages[partnerUsername]) { $scope.messages[partnerUsername] = []; }
        $scope.messages[partnerUsername].push(optimisticMessage);
        $timeout(scrollToBottom, 50);

        // Clear Input & Trigger Digest
        $scope.chatInput.text = '';
        $timeout(angular.noop); // Ensures view updates after clearing model

        // API Call
        const payload = { sender: senderUsername, recipient: partnerUsername, content: messageContent };
        console.log("[Send] Attempting to send API request with payload:", payload);
        Restangular.all('chat/messages').post(payload)
            .then(function(response) { /* ... update optimistic message ... */
                 console.log("[Send] Message sent successfully:", response);
                 const messageIndex = $scope.messages[partnerUsername].findIndex(m => m.id === optimisticMessage.id);
                 if (messageIndex > -1) { /* ... replace temp msg with server msg ... */
                     response.timestamp = new Date(response.timestamp); response.content = response.content || "";
                     response.text = response.content; response.sender = response.senderUsername;
                     $scope.messages[partnerUsername][messageIndex] = { ...response, senderDisplayName: optimisticMessage.senderDisplayName };
                 } else { console.warn("[Send] Could not find optimistic message to update."); }
            })
            .catch(function(error) { /* ... handle error, mark message ... */
                console.error("[Send] Error sending message:", error); $scope.sendError = "Failed to send message.";
                const messageIndex = $scope.messages[partnerUsername].findIndex(m => m.id === optimisticMessage.id);
                if (messageIndex > -1) { /* ... mark as failed ... */
                    $scope.messages[partnerUsername][messageIndex].isSending = false; $scope.messages[partnerUsername][messageIndex].isError = true;
                    $scope.messages[partnerUsername][messageIndex].errorText = "Failed to send";
                } else { console.warn("[Send] Could not find optimistic message to mark failed."); }
            });
    };

    // --- Utility Functions (No changes needed here) ---
    function scrollToBottom() {
        var chatArea = document.getElementById('chatMessagesArea');
        if (chatArea) { $timeout(function() { chatArea.scrollTop = chatArea.scrollHeight; }, 0); }
    }

    // --- Initialization and Watchers (Modified for polling cleanup) ---
    var unwatchUserInfo = $rootScope.$watch('userInfo', function(newUserInfo, oldUserInfo) {
        console.log("[Watcher] userInfo changed:", newUserInfo);
        if (newUserInfo && newUserInfo.username && (!oldUserInfo || newUserInfo.username !== oldUserInfo.username)) {
            $scope.currentUser = newUserInfo;
            $scope.loggedInUsername = $scope.currentUser.username;
            console.log('[Watcher] Updated loggedInUsername:', $scope.loggedInUsername);
            stopPolling(); // Stop polling if user changes (might re-select later)
            $scope.selectedUserToChatWith = null; // Deselect user on identity change
            $scope.messages = {}; // Clear messages
            initializeUsers();
        }
        else if (!newUserInfo && oldUserInfo) {
             console.log("[Watcher] User logged out.");
             stopPolling(); // Stop polling on logout
             $scope.currentUser = null; $scope.loggedInUsername = null;
             $scope.availableUsers = []; $scope.selectedUserToChatWith = null;
             $scope.messages = {}; $scope.sendError = null; $scope.chatInput.text = '';
        }
        else if (newUserInfo && !newUserInfo.username) {
            console.warn("[Watcher] UserInfo exists but lacks username.");
            stopPolling(); // Stop polling if user becomes invalid
            $scope.loggedInUsername = null;
            $scope.selectedUserToChatWith = null;
        }
    });

     // Initial check
     if ($scope.currentUser && $scope.currentUser.username) {
        console.log("[Init Check] UserInfo already available on load.");
        initializeUsers();
     } else { /* ... handle no initial user ... */ }

    // --- Cleanup (Modified for polling) ---
    $scope.$on('$destroy', function() {
        console.log("[Destroy] Cleaning up UserGroupDefaultChatCtrl.");
        unwatchUserInfo();
        stopPolling(); // <-- Make sure polling stops when controller is destroyed
    });

    // --- Real-time Receiving (Placeholder - Polling is the temporary alternative) ---
    // function handleIncomingMessage(message) { ... }

});