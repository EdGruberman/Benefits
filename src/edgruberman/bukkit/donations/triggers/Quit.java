package edgruberman.bukkit.donations.triggers;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;

import edgruberman.bukkit.donations.Command;

public class Quit extends PlayerEventTrigger {

    public Quit(final Command command, final ConfigurationSection definition) {
        super(command, definition);
    }

    @EventHandler
    public void onEvent(final PlayerQuitEvent quit) {
        this.dispatch(quit.getPlayer());
    }

}