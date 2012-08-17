package edgruberman.bukkit.donations;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.Date;
import java.util.logging.Handler;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import edgruberman.bukkit.donations.commands.Benefits;
import edgruberman.bukkit.donations.commands.History;
import edgruberman.bukkit.donations.commands.Process;
import edgruberman.bukkit.donations.commands.Reload;
import edgruberman.bukkit.donations.commands.Remove;
import edgruberman.bukkit.donations.messaging.ConfigurationCourier;
import edgruberman.bukkit.donations.messaging.Courier;

public final class Main extends JavaPlugin {

    private static final Version MINIMUM_CONFIGURATION = new Version("0.0.0a72");
    private static final Version MINIMUM_PACKAGES = new Version("0.0.0a72");

    public static Courier courier;

    private Coordinator coordinator;

    @Override
    public void onEnable() {
        this.reloadConfig();
        Main.courier = new ConfigurationCourier(this);

        // initialize offline player names with proper casing in cache
        Bukkit.getServer().getOfflinePlayers();

        final File pending = new File(this.getDataFolder(), this.getConfig().getString("pending"));
        final File incoming = new File(this.getDataFolder(), this.getConfig().getString("incoming"));
        final File processed = new File(this.getDataFolder(), this.getConfig().getString("processed"));
        this.coordinator = new Coordinator(this, this.loadConfig("packages.yml", Main.MINIMUM_PACKAGES), this.getConfig().getInt("period"), pending, incoming, processed);

        this.getCommand("donations:history").setExecutor(new History(this.coordinator));
        this.getCommand("donations:benefits").setExecutor(new Benefits(this.coordinator));
        this.getCommand("donations:process").setExecutor(new Process(this.coordinator));
        this.getCommand("donations:remove").setExecutor(new Remove(this.coordinator));
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

    @Override
    public void reloadConfig() {
        this.loadConfig("config.yml", Main.MINIMUM_CONFIGURATION);
        super.reloadConfig();
        this.setLogLevel(this.getConfig().getString("logLevel"));
    }

    @Override
    public void saveDefaultConfig() {
        this.extractConfig("config.yml", false);
    }

    private Configuration loadConfig(final String resource, final Version required) {
        // extract default if not existing
        this.extractConfig(resource, false);

        final File existing = new File(this.getDataFolder(), resource);
        final Configuration config = YamlConfiguration.loadConfiguration(existing);
        if (required == null) return config;

        // verify required or later version
        final Version version = new Version(config.getString("version"));
        if (version.compareTo(required) >= 0) return config;

        this.archiveConfig(resource, version);

        // extract default and reload
        return this.loadConfig(resource, null);
    }

    private void extractConfig(final String resource, final boolean replace) {
        final Charset source = Charset.forName("UTF-8");
        final Charset target = Charset.defaultCharset();
        if (target.equals(source)) {
            super.saveResource(resource, replace);
            return;
        }

        final File config = new File(this.getDataFolder(), resource);
        if (config.exists()) return;

        final char[] cbuf = new char[1024]; int read;
        try {
            final Reader in = new BufferedReader(new InputStreamReader(this.getResource(resource), source));
            final Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(config), target));
            while((read = in.read(cbuf)) > 0) out.write(cbuf, 0, read);
            out.close(); in.close();

        } catch (final Exception e) {
            throw new IllegalArgumentException("Could not extract configuration file \"" + resource + "\" to " + config.getPath() + "\";" + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    private void archiveConfig(final String resource, final Version version) {
        final String backupName = "{0} - Archive version {1} - {2,date,yyyyMMddHHmmss}.yml";
        final File backup = new File(this.getDataFolder(), MessageFormat.format(backupName, resource.replaceAll("(?i)\\.yml$", ""), version, new Date()));
        final File existing = new File(this.getDataFolder(), resource);

        if (!existing.renameTo(backup))
            throw new IllegalStateException("Unable to archive configuration file \"" + existing.getPath() + "\" with version \"" + version + "\" to \"" + backup.getPath() + "\"");

        this.getLogger().warning("Archived configuration file \"" + existing.getPath() + "\" with version \"" + version + "\" to \"" + backup.getPath() + "\"");
    }

    private void setLogLevel(final String name) {
        Level level;
        try { level = Level.parse(name); } catch (final Exception e) {
            level = Level.INFO;
            this.getLogger().warning("Log level defaulted to " + level.getName() + "; Unrecognized java.util.logging.Level: " + name + "; " + e);
        }

        // only set the parent handler lower if necessary, otherwise leave it alone for other configurations that have set it
        for (final Handler h : this.getLogger().getParent().getHandlers())
            if (h.getLevel().intValue() > level.intValue()) h.setLevel(level);

        this.getLogger().setLevel(level);
        this.getLogger().config("Log level set to: " + this.getLogger().getLevel());
    }

}
