package net.mikeyt.logic;

import net.mikeyt.model.Message;

public interface IMessageReservoir {
    Message getNextMessage();
}
