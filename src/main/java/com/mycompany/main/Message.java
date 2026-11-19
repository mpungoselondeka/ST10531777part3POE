package com.mycompany.main;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class Message {

    public static final String RECIPIENT_SUCCESS =
            "Cell phone number successfully captured.";

    public static final String RECIPIENT_FAILURE =
            "Cell phone number is incorrectly formatted "
            + "or does not contain an international code. "
            + "Please correct the number and try again.";

    public static final String LENGTH_OK = "Message ready to send.";

    public static final String SENT_SUCCESS = "Message successfully sent.";
    public static final String DELETE_PROMPT = "Press 0 to delete the message.";
    public static final String DISREGARDED = "Message deleted.";
    public static final String STORED = "Message successfully stored.";
    public static final String STORE_FAILED = "Message could not be stored.";
    public static final String INVALID_OPTION = "Invalid option.";

    public static final String FLAG_SENT = "Sent";
    public static final String FLAG_STORED = "Stored";
    public static final String FLAG_DISREGARD = "Disregard";

    public static final int MAX_MESSAGE_LENGTH = 250;

    private static String jsonFile = "storedMessages.json";

    private static final Random random = new Random();

    // ---------- Part 3 arrays (filled at runtime, never hard-coded) ----------
    public static final ArrayList<Message> sentMessages = new ArrayList<>();
    public static final ArrayList<Message> disregardedMessages = new ArrayList<>();
    public static final ArrayList<Message> storedMessages = new ArrayList<>();
    public static final ArrayList<String> messageHashes = new ArrayList<>();
    public static final ArrayList<String> messageIDs = new ArrayList<>();
    // every message of any flag (used to search by ID / recipient)
    public static final ArrayList<Message> allMessages = new ArrayList<>();

    // Set by Main after registration
    public static String currentSender = "Developer";

    // Counts every message the user has entered (sent, stored or disregarded)
    private static int messagesEntered = 0;

    // Next message number for the hash (first message is 0)
    private static int nextMessageNumber = 0;

    private final String messageID;
    private final int messageNumber;
    private final String sender;
    private final String recipient;
    private final String messageText;
    private final String messageHash;

    // Normal constructor: the ID and message number are auto generated
    public Message(String recipient, String messageText) {
        this(generateMessageID(), nextMessageNumber++,
                currentSender, recipient, messageText);
        messagesEntered++;
    }

    // Constructor that lets the caller supply the ID and message number.
    // Used by the unit tests so the hash can be checked against known values.
    public Message(String messageID, int messageNumber,
            String recipient, String messageText) {
        this(messageID, messageNumber, currentSender, recipient, messageText);
    }

    // Full constructor (also used when reading the JSON file)
    public Message(String messageID, int messageNumber, String sender,
            String recipient, String messageText) {
        this.messageID = messageID;
        this.messageNumber = messageNumber;
        this.sender = sender;
        this.recipient = recipient;
        this.messageText = messageText;
        this.messageHash = createMessageHash();
    }

    // Generates a unique ten-digit message ID (leading zeros allowed)
    private static String generateMessageID() {
        String id;

        do {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 10; i++) {
                sb.append(random.nextInt(10));
            }
            id = sb.toString();
        } while (idExists(id));

        return id;
    }

    private static boolean idExists(String id) {
        return messageIDs.contains(id);
    }

    // Ensures the message ID is not more than ten characters
    public boolean checkMessageID() {
        return messageID != null && messageID.length() <= 10;
    }

    // Returns the "Message ID is created" text
    public String returnMessageIDGenerated() {
        return "Message ID generated: " + messageID;
    }

    // Ensures the message is not more than 250 characters
    public static String checkMessageLength(String text) {
        int length = (text == null) ? 0 : text.length();

        if (length <= MAX_MESSAGE_LENGTH) {
            return LENGTH_OK;
        }

        return "Message exceeds 250 characters by "
                + (length - MAX_MESSAGE_LENGTH)
                + "; please reduce the size.";
    }

    // Ensures the cell number starts with an international code
    // (+ and 1 to 3 digits, e.g. +27) followed by no more than
    // ten digits, e.g. +27831234567. That means 4 to 13 digits
    // after the "+" in total, so a bare code like "+27" is rejected.
    public static String checkRecipientCell(String cell) {
        if (cell != null
                && cell.matches("^\\+[0-9]{4,13}$")) {
            return RECIPIENT_SUCCESS;
        }

        return RECIPIENT_FAILURE;
    }

    // Creates the message hash, e.g. 00:0:HITONIGHT
    public String createMessageHash() {
        String[] words = messageText.trim().split("\\s+");

        String firstWord = words[0].replaceAll("[^A-Za-z0-9]", "");
        String lastWord = words[words.length - 1].replaceAll("[^A-Za-z0-9]", "");

        String hash = messageID.substring(0, 2) + ":"
                + messageNumber + ":"
                + firstWord + lastWord;

        return hash.toUpperCase();
    }

    // ================= array handling =================

    // Puts a message in the array for its flag, plus the master, hash and
    // ID arrays. Does NOT write the JSON file (used by tests and file loading).
    public static void addToArrays(Message m, String flag) {
        if (FLAG_SENT.equals(flag)) {
            sentMessages.add(m);
        } else if (FLAG_STORED.equals(flag)) {
            storedMessages.add(m);
        } else {
            disregardedMessages.add(m);
        }
        allMessages.add(m);
        messageHashes.add(m.messageHash);
        messageIDs.add(m.messageID);
    }

    private void removeFromArrays() {
        sentMessages.remove(this);
        disregardedMessages.remove(this);
        storedMessages.remove(this);
        allMessages.remove(this);
        messageHashes.remove(messageHash);
        messageIDs.remove(messageID);
    }

    // Performs the chosen action:
    // 1 = send, 2 = disregard, 3 = store to send later
    public String SentMessage(int choice) {
        switch (choice) {
            case 1:
                addToArrays(this, FLAG_SENT);
                return SENT_SUCCESS;

            case 2:
                return DELETE_PROMPT;

            case 3:
                return storeMessage() ? STORED : STORE_FAILED;

            default:
                return INVALID_OPTION;
        }
    }

    // Lets the user choose to send, disregard or store the message
    public String SentMessage(Scanner input) {
        while (true) {
            System.out.println();
            System.out.println("What would you like to do with this message?");
            System.out.println("1) Send Message");
            System.out.println("2) Disregard Message");
            System.out.println("3) Store Message to send later");
            System.out.print("Choose an option: ");

            String choice = input.nextLine().trim();

            if (choice.equals("1")) {
                return SentMessage(1);

            } else if (choice.equals("2")) {
                System.out.println(SentMessage(2));
                System.out.print("> ");
                String confirm = input.nextLine().trim();

                if (confirm.equals("0")) {
                    addToArrays(this, FLAG_DISREGARD);   // keep it in the disregarded array
                    return DISREGARDED;
                }
                // anything else: go back to the options

            } else if (choice.equals("3")) {
                return SentMessage(3);

            } else {
                System.out.println("Invalid option. Please enter 1, 2 or 3.");
            }
        }
    }

    // Returns all the messages sent while the program is running
    public static String printMessages() {
        if (sentMessages.isEmpty()) {
            return "No messages have been sent.";
        }

        StringBuilder sb = new StringBuilder();

        for (Message m : sentMessages) {
            sb.append(m.getDetails()).append("\n");
        }

        return sb.toString();
    }

    // Total number of messages sent
    public static int returnTotalMessages() {
        return sentMessages.size();
    }

    public static int returnTotalStoredMessages() {
        return storedMessages.size();
    }

    // Total number of messages the user has entered (for the message limit)
    public static int returnTotalEntered() {
        return messagesEntered;
    }

    // Clears all saved messages and counters (used by the unit tests)
    public static void resetMessages() {
        sentMessages.clear();
        disregardedMessages.clear();
        storedMessages.clear();
        messageHashes.clear();
        messageIDs.clear();
        allMessages.clear();
        messagesEntered = 0;
        nextMessageNumber = 0;
    }

    // Changes the JSON file used by storeMessage (used by the unit tests)
    public static void setJsonFile(String path) {
        jsonFile = path;
    }

    // ================= stored messages (Part 3, 2a - 2f) =================

    // a. sender and recipient of every stored message
    public static String displaySenderAndRecipient() {
        if (storedMessages.isEmpty()) {
            return "There are no stored messages.";
        }

        StringBuilder sb = new StringBuilder();
        for (Message m : storedMessages) {
            sb.append("Sender: ").append(m.sender)
              .append(" | Recipient: ").append(m.recipient).append("\n");
        }
        return sb.toString().trim();
    }

    // b. longest message in a list (the menu passes the stored messages)
    public static String longestMessage(List<Message> list) {
        Message longest = null;

        for (Message m : list) {
            if (longest == null
                    || m.messageText.length() > longest.messageText.length()) {
                longest = m;
            }
        }

        return longest == null ? "No messages available." : longest.messageText;
    }

    // c. search for a message ID (null if not found)
    public static Message searchByMessageID(String id) {
        for (Message m : allMessages) {
            if (m.messageID.equals(id)) {
                return m;
            }
        }
        return null;
    }

    // d. every sent or stored message for one recipient
    public static ArrayList<String> searchByRecipient(String recipient) {
        ArrayList<String> results = new ArrayList<>();

        for (Message m : allMessages) {
            boolean sentOrStored = sentMessages.contains(m) || storedMessages.contains(m);
            if (sentOrStored && m.recipient.equals(recipient)) {
                results.add(m.messageText);
            }
        }
        return results;
    }

    // d (menu). messages STORED for one recipient (matches the menu wording)
    public static ArrayList<String> searchStoredByRecipient(String recipient) {
        ArrayList<String> results = new ArrayList<>();

        for (Message m : storedMessages) {
            if (m.recipient.equals(recipient)) {
                results.add(m.messageText);
            }
        }
        return results;
    }

    // e. delete a message using its hash
    public static String deleteByHash(String hash) {
        for (Message m : allMessages) {
            if (m.messageHash.equals(hash)) {
                boolean wasStored = storedMessages.contains(m);
                String text = m.messageText;
                m.removeFromArrays();

                if (wasStored) {
                    writeStoredJson();
                }
                return "Message: \"" + text + "\" successfully deleted.";
            }
        }
        return "No message found with that hash.";
    }

    // f. full details of every stored message
    public static String displayReport() {
        if (storedMessages.isEmpty()) {
            return "There are no stored messages.";
        }

        StringBuilder sb = new StringBuilder("===== STORED MESSAGES REPORT =====\n");
        for (Message m : storedMessages) {
            sb.append(m.getDetails()).append("\n");
        }
        return sb.toString().trim();
    }

    // ================= JSON file =================

    // Stores the message in a JSON file
    public boolean storeMessage() {
        addToArrays(this, FLAG_STORED);

        if (writeStoredJson()) {
            return true;
        }

        removeFromArrays();
        return false;
    }

    // Plain record used for the JSON file (one entry per stored message)
    private static class StoredRecord {
        String messageID;
        int messageNumber;
        String sender;
        String recipient;
        String message;
        String messageHash;
    }

    // Writes the whole Stored Messages array to the JSON file using Gson
    private static boolean writeStoredJson() {
        List<StoredRecord> records = new ArrayList<>();

        for (Message m : storedMessages) {
            StoredRecord r = new StoredRecord();
            r.messageID = m.messageID;
            r.messageNumber = m.messageNumber;
            r.sender = m.sender;
            r.recipient = m.recipient;
            r.message = m.messageText;
            r.messageHash = m.messageHash;
            records.add(r);
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try (FileWriter writer = new FileWriter(jsonFile)) {
            gson.toJson(records, writer);
            return true;

        } catch (IOException e) {
            System.out.println("Error saving JSON file: " + e.getMessage());
            return false;
        }
    }

    // Reads the JSON file into the Stored Messages array using Gson
    // (call once at start-up)
    public static void loadStoredMessages() {
        File f = new File(jsonFile);
        if (!f.exists()) {
            return;
        }

        Type listType = new TypeToken<ArrayList<StoredRecord>>() { }.getType();
        ArrayList<StoredRecord> records;

        try (FileReader reader = new FileReader(f)) {
            records = new Gson().fromJson(reader, listType);

        } catch (IOException | JsonParseException e) {
            System.out.println("Error reading JSON file: " + e.getMessage());
            return;
        }

        if (records == null) {
            return;
        }

        int highest = nextMessageNumber - 1;

        for (StoredRecord r : records) {
            if (r.messageID == null || r.message == null || r.message.trim().isEmpty()
                    || messageIDs.contains(r.messageID)) {
                continue;   // skip bad or already-loaded entries
            }

            String sender = (r.sender == null) ? currentSender : r.sender;

            addToArrays(new Message(r.messageID, r.messageNumber, sender,
                    r.recipient, r.message), FLAG_STORED);
            highest = Math.max(highest, r.messageNumber);
        }

        nextMessageNumber = highest + 1;
    }

    // ================= getters =================

    public String getMessageID() { return messageID; }
    public String getSender() { return sender; }
    public String getRecipient() { return recipient; }
    public String getMessageText() { return messageText; }
    public String getMessageHash() { return messageHash; }

    // Message details in the required order:
    // Message ID, Message Hash, Recipient, Message
    public String getDetails() {
        return "---------------------------\n"
                + "Message ID:   " + messageID + "\n"
                + "Message Hash: " + messageHash + "\n"
                + "Sender:       " + sender + "\n"
                + "Recipient:    " + recipient + "\n"
                + "Message:      " + messageText + "\n"
                + "---------------------------";
    }
}