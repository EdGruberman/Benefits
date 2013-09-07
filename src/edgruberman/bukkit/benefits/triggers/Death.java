package edgruberman.bukkit.benefits.triggers;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;

import edgruberman.bukkit.benefits.Command;

public final class Death extends PlayerEvent {

    public Death(final Command command, final ConfigurationSection definition) {
        super(command, definition);
    }

    @EventHandler
    public void onEvent(final PlayerDeathEvent death) {
        this.apply(death.getEntity());
    }

}
