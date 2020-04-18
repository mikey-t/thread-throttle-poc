package net.mikeyt.logic;

import lombok.SneakyThrows;
import net.mikeyt.model.Message;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.text.CharacterPredicates;
import org.apache.commons.text.RandomStringGenerator;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class MessageProviderMock implements IMessageProvider {
    private static final Logger log = LoggerFactory.getLogger(MessageProviderMock.class);
    private static final int BODY_LENGTH = 60;
    private static final Random rand = new Random();
    private static final RandomStringGenerator stringGenerator = new RandomStringGenerator.Builder()
            .withinRange('0', 'z')
            .filteredBy(CharacterPredicates.DIGITS, CharacterPredicates.LETTERS)
            .usingRandom(rand::nextInt)
            .build();
    private static final long MILLIS_UNTIL_ZERO_IS_POSSIBLE = 30000;

    private final StopWatch sw = new StopWatch();

    public MessageProviderMock() {
        sw.start();
    }

    public static Message createMessage() {
        return new Message(UUID.randomUUID().toString(), stringGenerator.generate(BODY_LENGTH), DateTime.now());
    }

    @SneakyThrows
    @Override
    public List<Message> getMessages(int max) {
        Thread.sleep(50);
        int numMessages = rand.nextInt(max);
        if (numMessages == 0 && sw.getTime() < MILLIS_UNTIL_ZERO_IS_POSSIBLE) {
            numMessages++;
        }

        List<Message> messages = new ArrayList<>();
        for (int i = 0; i < numMessages; i++) {
            messages.add(createMessage());
        }
        log.info("Message provider returning " + numMessages + " messages");
        return messages;
    }
}
