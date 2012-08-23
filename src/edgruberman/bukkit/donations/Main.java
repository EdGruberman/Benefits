package edgruberman.bukkit.donations;

import java.io.File;

import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;

import edgruberman.bukkit.donations.commands.Benefits;
import edgruberman.bukkit.donations.commands.History;
import edgruberman.bukkit.donations.commands.Process;
import edgruberman.bukkit.donations.commands.Reload;
import edgruberman.bukkit.donations.commands.Undo;
import edgruberman.bukkit.donations.messaging.ConfigurationCourier;
import edgruberman.bukkit.donations.messaging.Courier;
import edgruberman.bukkit.donations.util.CustomPlugin;

public final class Main extends CustomPlugin {

    public static Courier courier;

    private Coordinator coordinator;

    @Override
    public void onLoad() {
        this.putConfigMinimum("config.yml", "0.0.0a72");
        this.putConfigMinimum("packages.yml", "0.0.0a72");
    }

    @Override
    public void onEnable() {
        this.reloadConfig();
        Main.courier = new ConfigurationCourier(this);

        // initialize offline player names with proper casing in cache
        Bukkit.getServer().getOfflinePlayers();

        final File pending = new File(this.getDataFolder(), this.getConfig().getString("pending"));
        final File incoming = new File(this.getDataFolder(), this.getConfig().getString("incoming"));
        final File processed = new File(this.getDataFolder(), this.getConfig().getString("processed"));
        this.coordinator = new Coordinator(this, this.loadConfig("packages.yml"), this.getConfig().getInt("period"), pending, incoming, processed);

        this.getCommand("donations:history").setExecutor(new History(this.coordinator));
        this.getCommand("donations:benefits").setExecutor(new Benefits(this.coordinator));
        this.getCommand("donations:process").setExecutor(new Process(this.coordinator));
        this.getCommand("donations:undo").setExecutor(new Undo(this.coordinator));
        this.getCommand("donations:reload").setExecutor(new Reload(this));
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
        Bukkit.getScheduler().cancelTasks(this);

        this.coordinator.clear();
        this.coordinator = null;

        Main.courier = null;
    }

}
