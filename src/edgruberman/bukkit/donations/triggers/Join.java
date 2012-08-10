package edgruberman.bukkit.donations.triggers;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;

import edgruberman.bukkit.donations.Command;

/** Player's next join */
public class Join extends PlayerEventTrigger {

    public Join(final Command command, final ConfigurationSection definition) {
        super(command, definition);
    }

    @EventHandler
    public void onEvent(final PlayerJoinEvent join) {
        this.dispatch(join.getPlayer());
    }

}