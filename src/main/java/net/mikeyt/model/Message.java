package net.mikeyt.model;

import lombok.Data;
import org.joda.time.DateTime;

@Data
public class Message {
    private String id;
    private String body;
    private DateTime timeReceived; // Use for visibility timeout refresh
    private long mockProcessingMillis;

    public Message(String id, String body, DateTime timeReceived, long mockProcessingMillis) {
        this.id = id;
        this.body = body;
        this.timeReceived = timeReceived;
        this.mockProcessingMillis = mockProcessingMillis;
    }
}
