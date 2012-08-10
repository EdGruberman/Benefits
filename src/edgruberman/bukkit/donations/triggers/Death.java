package edgruberman.bukkit.donations.triggers;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;

import edgruberman.bukkit.donations.Command;

public final class Death extends PlayerEventTrigger {

    public Death(final Command command, final ConfigurationSection definition) {
        super(command, definition);
    }

    @EventHandler
    public void onEvent(final PlayerDeathEvent death) {
        this.dispatch(death.getEntity());
    }

}
