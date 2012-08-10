package edgruberman.bukkit.donations.triggers;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerRespawnEvent;

import edgruberman.bukkit.donations.Command;

public class Respawn extends PlayerEventTrigger {

    public Respawn(final Command command, final ConfigurationSection definition) {
        super(command, definition);
    }

    @EventHandler
    public void onEvent(final PlayerRespawnEvent respawn) {
        this.dispatch(respawn.getPlayer());
    }

}