package net.mikeyt.model;

import lombok.Data;
import org.joda.time.DateTime;

@Data
public class Message {
    private String id;
    private String body;
    private DateTime timeReceived; // Use for visibility timeout refresh

    public Message(String id, String body, DateTime timeReceived) {
        this.id = id;
        this.body = body;
        this.timeReceived = timeReceived;
    }
}
