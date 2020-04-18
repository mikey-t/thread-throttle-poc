package net.mikeyt.logic;

import lombok.SneakyThrows;
import net.mikeyt.model.Message;
import org.apache.commons.lang3.RandomUtils;
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
    private static final long MILLIS_UNTIL_ZERO_MESSAGES_RESULT_IS_POSSIBLE = 10000;
    private static final float PERCENT_SLOW = 0.05f;
    private static final long FAST_MIN_MILLIS = 50;
    private static final long FAST_MAX_MILLIS = 500;
    private static final long SLOW_MIN_MILLIS = 4000;
    private static final long SLOW_MAX_MILLIS = 6000;
    private static final long PROCESSING_TIME_MIN = 25;
    private static final long PROCESSING_TIME_MAX = 250;

    private final StopWatch sw = new StopWatch();

    public MessageProviderMock() {
        sw.start();
    }

    @SneakyThrows
    @Override
    public List<Message> getMessages(int max) {
        Thread.sleep(RandomUtils.nextLong(PROCESSING_TIME_MIN, PROCESSING_TIME_MAX));

        int numMessages = RandomUtils.nextInt(sw.getTime() < MILLIS_UNTIL_ZERO_MESSAGES_RESULT_IS_POSSIBLE ? 1 : 0, max);

        List<Message> messages = new ArrayList<>();
        for (int i = 0; i < numMessages; i++) {
            messages.add(createMessage());
        }

        log.info("Message provider mock will return " + numMessages + " messages");
        return messages;
    }

    private static Message createMessage() {
        return new Message(UUID.randomUUID().toString(), stringGenerator.generate(BODY_LENGTH), DateTime.now(), getMockProcessingTimeMillis());
    }

    private static long getMockProcessingTimeMillis() {
        boolean isSlow = rand.nextFloat() < PERCENT_SLOW;
        return isSlow? RandomUtils.nextLong(SLOW_MIN_MILLIS, SLOW_MAX_MILLIS) : RandomUtils.nextLong(FAST_MIN_MILLIS, FAST_MAX_MILLIS);
    }
}
