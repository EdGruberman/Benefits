package edgruberman.bukkit.donations.messaging;

import edgruberman.bukkit.donations.messaging.messages.Confirmation;

public interface Recipients {

    public abstract Confirmation send(Message message);

}
