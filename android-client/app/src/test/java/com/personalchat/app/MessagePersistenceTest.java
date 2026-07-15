package com.personalchat.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.personalchat.app.data.model.DeliveryStatus;
import com.personalchat.app.data.model.Message;

import org.junit.Test;

public class MessagePersistenceTest {

    @Test
    public void testMessageModel_initialization() {
        String msgId = "msg-123";
        String convId = "conv-abc";
        String sender = "peer-sender";
        String receiver = "peer-receiver";
        String content = "Hello world";
        long timestamp = 1234567890L;

        Message message = new Message(
                msgId,
                convId,
                sender,
                receiver,
                content,
                timestamp,
                DeliveryStatus.SENDING.name(),
                false
        );

        assertEquals(msgId, message.getId());
        assertEquals(convId, message.getConversationId());
        assertEquals(sender, message.getSenderId());
        assertEquals(receiver, message.getReceiverId());
        assertEquals(content, message.getContent());
        assertEquals(timestamp, message.getTimestamp());
        assertEquals(DeliveryStatus.SENDING.name(), message.getStatus());
        assertFalse(message.isIncoming());
    }

    @Test
    public void testMessageStatus_transitions() {
        Message message = new Message(
                "msg-999",
                "conv-xyz",
                "me",
                "peer",
                "Checking in",
                System.currentTimeMillis(),
                DeliveryStatus.SENDING.name(),
                false
        );

        // 1. Initial status: SENDING
        assertEquals(DeliveryStatus.SENDING.name(), message.getStatus());

        // 2. Transition to SENT (transmitted over WebRTC DataChannel)
        message.setStatus(DeliveryStatus.SENT.name());
        assertEquals(DeliveryStatus.SENT.name(), message.getStatus());

        // 3. Transition to DELIVERED (acknowledgement received from peer)
        message.setStatus(DeliveryStatus.DELIVERED.name());
        assertEquals(DeliveryStatus.DELIVERED.name(), message.getStatus());
    }
}
