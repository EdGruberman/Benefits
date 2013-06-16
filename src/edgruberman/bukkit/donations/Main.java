package edgruberman.bukkit.donations;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.configuration.ConfigurationSection;

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
    private static final String LANGUAGE_FILE = "language.yml";

    public static ConfigurationCourier courier;

    private Coordinator coordinator;
    private BufferedYamlConfiguration donations;
    private BufferedYamlConfiguration pending;
    private BufferedYamlConfiguration registrations;
    private final List<Processor> processors = new ArrayList<Processor>();

    @Override
    public void onLoad() {
        this.putConfigMinimum("0.0.0a109");
        this.putConfigMinimum(Main.LANGUAGE_FILE, "0.0.0a141");
    }

    @Override
    public void onEnable() {
        this.reloadConfig();
        Main.courier = ConfigurationCourier.create(this).setBase(this.loadConfig(Main.LANGUAGE_FILE)).setFormatCode("format-code").build();


        // coordinator
        this.coordinator = new Coordinator(this, this.getConfig().getString("currency"));
        this.coordinator.setSandbox(this.getConfig().getBoolean("sandbox"));
        this.getLogger().log(Level.CONFIG, "Currency: {0}; Sandbox: {1}", new Object[] { this.coordinator.currency, this.coordinator.isSandbox() });

        this.loadPackages(this.loadConfig(Main.PACKAGES_FILE));

        this.donations = new BufferedYamlConfiguration(this, new File(this.getDataFolder(), "donations.yml"), 5000);
        try { this.donations.load(); } catch (final Exception e) { throw new IllegalStateException("Unable to load donations.yml file; {0}", e); }
        this.loadDonations(this.donations);
        this.getLogger().log(Level.CONFIG, "Loaded {0} donation{0,choice,0#s|1#|2#s}", this.coordinator.donations.size());

        this.pending = new BufferedYamlConfiguration(this, new File(this.getDataFolder(), "pending.yml"), 5000);
        try { this.pending.load(); } catch (final Exception e) { throw new IllegalStateException("Unable to load pending.yml file; {0}", e); }
        this.loadPending(this.pending);

        this.registrations = new BufferedYamlConfiguration(this, new File(this.getDataFolder(), "registrations.yml"), 5000);
        try { this.registrations.load(); } catch (final Exception e) { throw new IllegalStateException("Unable to load registrations.yml file; {0}", e); }
        this.loadRegistrations(this.registrations.getRoot());
        this.getLogger().log(Level.CONFIG, "Loaded {0} registration{0,choice,0#s|1#|2#s}", this.coordinator.registrations.size());


        // payment processors
        final ConfigurationSection processorsConfig = this.getConfig().getConfigurationSection("processors");
        for (final String key : processorsConfig.getKeys(false)) {
            final ConfigurationSection config = processorsConfig.getConfigurationSection(key);
            if (!config.getBoolean("enable")) continue;

            final String processorClass = config.getString("class", key);
            final Processor processor;
            try {
                processor = Processor.create(processorClass, this.coordinator, config);
            } catch (final Exception e) {
                this.getLogger().log(Level.WARNING, "Failed to create Processor: {0}; {1}; {2}; {3}", new Object[] { processorClass, e, e.getCause(), ( e.getCause() != null ? e.getCause().getCause() : null ) });
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

    private void loadPackages(final ConfigurationSection packages) {
        for (final String name : packages.getKeys(false)) {
            if (name.equals("version")) continue;
            final Package pkg = new Package(this.coordinator, packages.getConfigurationSection(name));
            final Package duplicate = this.coordinator.putPackage(pkg);
            if (duplicate != null) this.getLogger().log(Level.WARNING, "Unable to load duplicate package from packages.yml; {0}", duplicate);
        }
    }

    private void loadDonations(final ConfigurationSection donations) {
        final String lower = this.coordinator.currency.toLowerCase();
        int mismatched = 0;

        for (final String key : donations.getKeys(false)) {
            final ConfigurationSection entry = donations.getConfigurationSection(key);

            final String currency = entry.getString("currency");
            if (!lower.equals(currency.toLowerCase())) {
                mismatched++;
                continue;
            }

            final Donation donation = new Donation(entry.getString("processor"), entry.getString("id"), entry.getString("origin")
                    , entry.getString("player"), currency, entry.getLong("amount"), entry.getLong("contributed"), ( entry.isSet("packages") ? entry.getStringList("packages") : null ));

            final Donation duplicate = this.coordinator.putDonation(donation);
            if (duplicate != null) this.getLogger().log(Level.WARNING, "Unable to load duplicate donation from donations.yml; {0}", duplicate);
        }

        if (mismatched > 0) this.getLogger().log(Level.WARNING, "Unable to load {0} currency mismatched donations from donations.yml; Required currency: {1}"
                , new Object[] { mismatched, this.coordinator.currency });
    }

    void saveDonation(final Donation donation) {
        final ConfigurationSection entry = this.donations.createSection(donation.getKey());
        entry.set("processor", donation.processor);
        entry.set("id", donation.id);
        entry.set("origin", donation.origin);
        entry.set("player", donation.player);
        entry.set("currency", donation.currency);
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
        this.pending.clear();

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
        for (final String path : registrations.getKeys(false)) {
            final ConfigurationSection entry = registrations.getConfigurationSection(path);
            this.coordinator.registrations.put(entry.getString("origin").toLowerCase(), entry.getString("player"));
        }
    }

    void saveRegistration(final String origin, final String player) {
        final ConfigurationSection entry = this.registrations.getRoot().createSection(UUID.randomUUID().toString());
        entry.set("origin", origin);
        entry.set("player", player);
        this.registrations.queueSave();
    }

}
