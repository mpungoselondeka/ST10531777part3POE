package com.mycompany.main;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MessageTest {

    static Message m1, m2, m3, m4, m5;

    // Populates the arrays from the five test data messages in the brief
    @BeforeEach
    void setUp() {
        Message.resetMessages();
        Message.setJsonFile("testStoredMessages.json");

        m1 = new Message("0000000001", 0, "+27834557896", "Did you get the cake?");
        m2 = new Message("0000000002", 1, "+27838884567",
                "Where are you? You are late! I have asked you to be on time.");
        m3 = new Message("0000000003", 2, "+27834484567", "Yohoooo, I am at your gate.");
        m4 = new Message("0000000004", 3, "0838884567", "It is dinner time!");
        m5 = new Message("0000000005", 4, "+27838884567", "Ok, I am leaving without you.");

        Message.addToArrays(m1, Message.FLAG_SENT);
        Message.addToArrays(m2, Message.FLAG_STORED);
        Message.addToArrays(m3, Message.FLAG_DISREGARD);
        Message.addToArrays(m4, Message.FLAG_SENT);
        Message.addToArrays(m5, Message.FLAG_STORED);
    }

    @Test
    void sentMessagesArrayCorrectlyPopulated() {
        ArrayList<String> texts = new ArrayList<>();
        for (Message m : Message.sentMessages) {
            texts.add(m.getMessageText());
        }
        assertEquals(Arrays.asList("Did you get the cake?", "It is dinner time!"), texts);
    }

    @Test
    void otherArraysPopulated() {
        assertEquals(1, Message.disregardedMessages.size());
        assertEquals(2, Message.storedMessages.size());
        assertEquals(5, Message.messageHashes.size());
        assertEquals(5, Message.messageIDs.size());
    }

    @Test
    void displayLongestMessage() {
        assertEquals("Where are you? You are late! I have asked you to be on time.",
                Message.longestMessage(Message.allMessages));
    }

    @Test
    void searchForMessageID() {
        Message found = Message.searchByMessageID("0000000004");
        assertNotNull(found);
        assertEquals("0838884567", found.getRecipient());
        assertEquals("It is dinner time!", found.getMessageText());
    }

    @Test
    void searchAllMessagesForRecipient() {
        assertEquals(Arrays.asList(
                "Where are you? You are late! I have asked you to be on time.",
                "Ok, I am leaving without you."),
                Message.searchByRecipient("+27838884567"));
    }

    @Test
    void deleteMessageUsingHash() {
        String hash = m2.getMessageHash();
        assertEquals("Message: \"Where are you? You are late! I have asked you to be on time.\" successfully deleted.",
                Message.deleteByHash(hash));
        assertFalse(Message.storedMessages.contains(m2));
        assertFalse(Message.messageHashes.contains(hash));
    }

    @Test
    void displayReportListsStoredMessageDetails() {
        String report = Message.displayReport();
        assertTrue(report.contains(m2.getMessageHash()));
        assertTrue(report.contains("+27838884567"));
        assertTrue(report.contains("Ok, I am leaving without you."));
    }
}