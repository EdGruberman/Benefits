package edgruberman.bukkit.donations;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

public final class Coordinator implements Runnable {

    private static final long LIMIT_TOLERANCE = 1000 * 60 * 60 * 12; // 12 hours

    public final Map<String, Package> packages = new HashMap<String, Package>();
    public final Map<String, Donation> processed = new HashMap<String, Donation>();

    public final Plugin plugin;
    private final int period;
    private final File pending;
    private final File incomingFolder;
    private final File processedFolder;
    private Integer taskId = null;

    Coordinator(final Plugin plugin, final ConfigurationSection packages, final int period, final File pending, final File incomingFolder, final File processedFolder) {
        this.plugin = plugin;
        this.period = period;
        this.pending = pending;
        this.incomingFolder = incomingFolder;
        this.processedFolder = processedFolder;

        this.loadPackages(packages);
        this.loadProcessed();
        this.loadPending();

        if (!this.incomingFolder.exists()) this.incomingFolder.mkdirs();
        if (!this.processedFolder.exists()) this.processedFolder.mkdirs();

        this.taskId = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this.plugin, this, this.period, this.period);
    }

    void clear() {
        if (this.taskId != null && this.taskId != -1)
            Bukkit.getServer().getScheduler().cancelTask(this.taskId);

        for (final Package pkg : this.packages.values()) pkg.clear();
        this.packages.clear();

        this.processed.clear();
    }

    public void process(final Donation donation) {
        // Ensure no duplicate processing even if a problem occurs
        this.processed.put(donation.getKey(), donation);

        final long now = System.currentTimeMillis();
        for (final Package pkg : this.applicable(donation.amount)) {
            // Identify most recent time this package was applied for player
            Long lastApplied = null;
            for(final Donation previous : this.history(donation.player))
                if (previous.packages.contains(pkg.name)) {
                    lastApplied = TimeUnit.MILLISECONDS.toDays(now - previous.contributed + Coordinator.LIMIT_TOLERANCE);
                    break;
                }

            // Do not re-apply package if last time package was applied is less than package defined limit
            if (pkg.limit != null && lastApplied != null && lastApplied < pkg.limit) {
                this.plugin.getLogger().finer("Package " + pkg.name + " applied only " + lastApplied + " day(s) ago (Limit: " + pkg.limit + ") for " + donation.player);
                continue;
            }

            donation.packages.add(pkg.name);
            for (final Benefit benefit : pkg.benefits.values())
                for (final Command command : benefit.commands.values())
                    command.add(donation);
        }

        // TODO create new file in processed with donation details
        final YamlConfiguration yml = new YamlConfiguration();
        yml.set("origin", donation.origin);
        yml.set("player", donation.player);
        yml.set("amount", donation.amount);
        yml.set("contributed", donation.contributed);
        yml.set("packages", donation.packages);
        final File saveAs = new File(this.processedFolder, donation.getKey() + ".yml");
        try {
            yml.save(saveAs);
        } catch (final IOException e) {
            this.plugin.getLogger().log(Level.SEVERE, "Unable to save " + donation.toString() + " as " + saveAs.getPath(), e);
        }

        this.savePending();
    }

    // TODO move bad/corrupt files to alternate folder for later manual review
    @Override
    public void run() {
        final File[] files = this.incomingFolder.listFiles();
        if (files == null || files.length == 0) return;

        for (final File file : files) {
            final YamlConfiguration entry = YamlConfiguration.loadConfiguration(file);
            final Donation donation = new Donation(entry.getString("origin"), entry.getString("player"), entry.getDouble("amount"), entry.getLong("contributed"), null);
            this.process(donation);
            file.delete();
        }
    }

    /** processed donations for a given player ordered by newest contribution first */
    public List<Donation> history(final String player) {
        final List<Donation> history = new ArrayList<Donation>();
        for (final Donation donation : this.processed.values())
            if (donation.player.equalsIgnoreCase(player))
                history.add(donation);

        Collections.sort(history, Donation.NEWEST_CONTRIBUTION_FIRST);
        return history;
    }

    /** packages to be applied for a given donation amount ordered by lowest minimum first */
    public List<Package> applicable(final double amount) {
        final List<Package> applicable = new ArrayList<Package>();
        for (final Package pkg : this.packages.values())
            if (amount >= pkg.minimum)
                applicable.add(pkg);

        Collections.sort(applicable, Package.LOWEST_MINIMUM_FIRST);
        return applicable;
    }

    // TODO check for duplicates while loading and send warning to log
    private void loadPackages(final ConfigurationSection packages) {
        for (final String packageName : packages.getKeys(false)) {
            if (packageName.equals("version")) continue;
            final Package pkg = new Package(this, packages.getConfigurationSection(packageName));
            this.packages.put(pkg.name.toLowerCase(), pkg);
        }
    }

    // TODO check for duplicates while loading and send warning to log
    private void loadProcessed() {
        for (final File file : this.processedFolder.listFiles()) {
            final YamlConfiguration entry = YamlConfiguration.loadConfiguration(file);

            final Donation donation = new Donation(entry.getString("origin"), entry.getString("player")
                    , entry.getDouble("amount"), entry.getLong("contributed"), entry.getStringList("packages"));

            this.processed.put(donation.getKey(), donation);
            this.plugin.getLogger().finest("Loaded processed donation: " + donation.getKey() + " = " + donation.toString());
        }
    }

    private void loadPending() {
        final ConfigurationSection pending = YamlConfiguration.loadConfiguration(this.pending);

        for (final String packageName : pending.getKeys(false)) {
            final ConfigurationSection benefits = pending.getConfigurationSection(packageName);

            for (final String benefitName : benefits.getKeys(false)) {
                final ConfigurationSection commands = benefits.getConfigurationSection(benefitName);

                for (final String commandName : commands.getKeys(false)) {

                    for (final String donationKey : commands.getStringList(commandName)) {
                        final Donation donation = this.processed.get(donationKey);
                        if (donation == null) {
                            this.plugin.getLogger().warning("Unable to find donation " + donationKey + " to add to " + packageName + "." + benefitName + "." + commandName);
                            continue;
                        }

                        final Package pkg = this.packages.get(packageName.toLowerCase());
                        if (pkg == null) {
                            this.plugin.getLogger().warning("Unable to find package " + packageName + " to add " + donationKey + " to " + packageName + "." + benefitName + "." + commandName);
                            continue;
                        }

                        final Benefit benefit = pkg.benefits.get(benefitName.toLowerCase());
                        if (benefit == null) {
                            this.plugin.getLogger().warning("Unable to find benefit " + benefitName + " to add " + donationKey + " to " + packageName + "." + benefitName + "." + commandName);
                            continue;
                        }

                        final Command command = benefit.commands.get(commandName.toLowerCase());
                        if (command == null) {
                            this.plugin.getLogger().warning("Unable to find command " + commandName + " to add " + donationKey + " to " + packageName + "." + benefitName + "." + commandName);
                            continue;
                        }

                        command.add(donation);
                    }
                }
            }
        }
    }

    public void savePending() {
        final YamlConfiguration pending = new YamlConfiguration();

        for (final Package pkg : this.packages.values())
            for (final Benefit benefit : pkg.benefits.values())
                for (final Command command : benefit.commands.values())
                    for (final Trigger trigger : command.triggers) {
                        final List<String> donations = new ArrayList<String>();
                        for (final Donation donation : trigger.getPending()) donations.add(donation.getKey());
                        if (donations.size() > 0) pending.set(pkg.name + "." + benefit.name + "." + command.name, donations);
                    }

        try {
            pending.save(this.pending);
        } catch (final IOException e) {
            throw new IllegalStateException("Unable to save pending file \"" + this.pending + "\"; " + e.getClass() + ": " + e.getMessage());
        }
    }

    static List<String> getStringList(final ConfigurationSection config, final String path) {
        if (config.isList(path))
            return config.getStringList(path);

        if (config.isString(path))
            return Arrays.asList(config.getString(path));

        return Collections.emptyList();
    }

}
