package edgruberman.bukkit.donations;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;

import edgruberman.bukkit.donations.commands.Benefits;
import edgruberman.bukkit.donations.commands.History;
import edgruberman.bukkit.donations.commands.Process;
import edgruberman.bukkit.donations.commands.Register;
import edgruberman.bukkit.donations.commands.Reload;
import edgruberman.bukkit.donations.commands.Sandbox;
import edgruberman.bukkit.donations.commands.Undo;
import edgruberman.bukkit.donations.messaging.ConfigurationCourier;
import edgruberman.bukkit.donations.util.BufferedYamlConfiguration;
import edgruberman.bukkit.donations.util.CustomPlugin;

public final class Main extends CustomPlugin {

    private static final String PACKAGES_FILE = "packages.yml";

    public static ConfigurationCourier courier;

    private Coordinator coordinator;
    private BufferedYamlConfiguration donations;
    private BufferedYamlConfiguration pending;
    private BufferedYamlConfiguration registrations;
    private final List<Processor> processors = new ArrayList<Processor>();

    @Override
    public void onLoad() {
        this.putConfigMinimum("0.0.0a106");
        this.putConfigMinimum(Main.PACKAGES_FILE, "0.0.0a84");
    }

    @Override
    public void onEnable() {
        this.reloadConfig();
        Main.courier = ConfigurationCourier.create(this).setBase(this.loadConfig("language.yml")).setFormatCode("format-code").build();


        // coordinator
        this.coordinator = new Coordinator(this);
        this.coordinator.setSandbox(this.getConfig().getBoolean("sandbox"));

        this.loadPackages(this.loadConfig(Main.PACKAGES_FILE));

        this.donations = new BufferedYamlConfiguration(this, new File(this.getDataFolder(), "donations.yml"), 5000);
        this.loadDonations(this.donations);
        this.getLogger().log(Level.CONFIG, "Loaded {0} donations", this.coordinator.donations.size());

        this.pending = new BufferedYamlConfiguration(this, new File(this.getDataFolder(), "pending.yml"), 5000);
        this.loadPending(this.pending);

        this.registrations = new BufferedYamlConfiguration(this, new File(this.getDataFolder(), "registrations.yml"), 5000);
        this.loadRegistrations(this.registrations);
        this.getLogger().log(Level.CONFIG, "Loaded {0} registrations", this.coordinator.registrations.size());


        // processors
        final ConfigurationSection processorsConfig = this.getConfig().getConfigurationSection("processors");
        for (final String key : processorsConfig.getKeys(false)) {
            final ConfigurationSection config = processorsConfig.getConfigurationSection(key);
            if (!config.getBoolean("enable")) continue;

            final String processorClass = config.getString("class", key);
            final Processor processor;
            try {
                processor = Processor.create(processorClass, this.coordinator, config);
            } catch (final Exception e) {
                this.getLogger().log(Level.WARNING, "Failed to create Processor: {0}; {1}", new Object[] { processorClass, e });
                this.getLogger().log(Level.FINE, "", e);
                continue;
            }
            this.processors.add(processor);
        }


        // commands
        this.getCommand("donations:history").setExecutor(new History(this.coordinator));
        this.getCommand("donations:benefits").setExecutor(new Benefits(this.coordinator));
        this.getCommand("donations:process").setExecutor(new Process(this.coordinator));
        this.getCommand("donations:undo").setExecutor(new Undo(this.coordinator));
        this.getCommand("donations:sandbox").setExecutor(new Sandbox(this.coordinator));
        this.getCommand("donations:register").setExecutor(new Register(this.coordinator));
        this.getCommand("donations:reload").setExecutor(new Reload(this));
    }

    @Override
    public void onDisable() {
        for (final Processor processor : this.processors) processor.stop();
        this.processors.clear();

        this.coordinator.clear();
        this.coordinator = null;

        Main.courier = null;
    }


    // -- repository methods --

    // TODO check for duplicates while loading and send warning to log
    private void loadPackages(final ConfigurationSection packages) {
        for (final String name : packages.getKeys(false)) {
            if (name.equals("version")) continue;
            final Package pkg = new Package(this.coordinator, packages.getConfigurationSection(name));
            this.coordinator.putPackage(pkg);
        }
    }

    // TODO check for duplicates while loading and send warning to log
    private void loadDonations(final ConfigurationSection donations) {
        for (final String key : donations.getKeys(false)) {
            final ConfigurationSection entry = donations.getConfigurationSection(key);

            final Donation donation = new Donation(entry.getString("processor"), entry.getString("id"), entry.getString("origin")
                    , entry.getString("player") , entry.getLong("amount"), entry.getLong("contributed"), entry.getStringList("packages"));

            this.coordinator.putDonation(donation);
        }
    }

    void saveDonation(final Donation donation) {
        final ConfigurationSection entry = this.donations.createSection(donation.getKey());
        entry.set("processor", donation.processor);
        entry.set("id", donation.id);
        entry.set("origin", donation.origin);
        entry.set("player", donation.player);
        entry.set("amount", donation.amount);
        entry.set("contributed", donation.contributed);
        entry.set("packages", donation.packages);
        this.donations.queueSave();
    }

    private void loadPending(final ConfigurationSection pending) {
        for (final String packageName : pending.getKeys(false)) {
            final ConfigurationSection benefits = pending.getConfigurationSection(packageName);

            for (final String benefitName : benefits.getKeys(false)) {
                final ConfigurationSection commands = benefits.getConfigurationSection(benefitName);

                for (final String commandName : commands.getKeys(false)) {

                    for (final String donationKey : commands.getStringList(commandName)) {
                        final Donation donation = this.coordinator.getDonation(donationKey);
                        if (donation == null) {
                            this.getLogger().warning("Unable to find donation " + donationKey + " to add to " + packageName + "." + benefitName + "." + commandName);
                            continue;
                        }

                        final Package pkg = this.coordinator.getPackage(packageName.toLowerCase());
                        if (pkg == null) {
                            this.getLogger().warning("Unable to find package " + packageName + " to add " + donationKey + " to " + packageName + "." + benefitName + "." + commandName);
                            continue;
                        }

                        final Benefit benefit = pkg.benefits.get(benefitName.toLowerCase());
                        if (benefit == null) {
                            this.getLogger().warning("Unable to find benefit " + benefitName + " to add " + donationKey + " to " + packageName + "." + benefitName + "." + commandName);
                            continue;
                        }

                        final Command command = benefit.commands.get(commandName.toLowerCase());
                        if (command == null) {
                            this.getLogger().warning("Unable to find command " + commandName + " to add " + donationKey + " to " + packageName + "." + benefitName + "." + commandName);
                            continue;
                        }

                        command.add(donation);
                    }
                }
            }
        }
    }

    void savePending() {
        try {
            this.pending.loadFromString("");
        } catch (final InvalidConfigurationException e) {
            throw new RuntimeException(e);
        }

        for (final Package pkg : this.coordinator.packages.values()) {
            for (final Benefit benefit : pkg.benefits.values()) {
                for (final Command command : benefit.commands.values()) {
                    for (final Trigger trigger : command.triggers) {
                        final List<String> donations = new ArrayList<String>();
                        for (final Donation donation : trigger.getPending()) donations.add(donation.getKey());
                        if (donations.size() > 0) this.pending.set(pkg.name + "." + benefit.name + "." + command.name, donations);
                    }
                }
            }
        }

        this.pending.queueSave();
    }

    private void loadRegistrations(final ConfigurationSection registrations) {
        final ConfigurationSection entries = registrations.getConfigurationSection("registrations");
        if (entries == null) return;

        for (final String origin : entries.getKeys(false)) {
            this.coordinator.registrations.put(origin.toLowerCase(), entries.getString(origin));
        }
    }

    void saveRegistration(final String origin, final String player) {
        this.registrations.set("registrations." + origin, player);
        this.registrations.queueSave();
    }

}
