package net.mikeyt.logic;

import net.mikeyt.model.Message;

import java.util.List;

public interface IMessageProvider {
    List<Message> getMessages(int max);
}
