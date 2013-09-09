package edgruberman.bukkit.benefits;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.configuration.ConfigurationSection;

import edgruberman.bukkit.benefits.commands.Assign;
import edgruberman.bukkit.benefits.commands.Benefits;
import edgruberman.bukkit.benefits.commands.Contributions;
import edgruberman.bukkit.benefits.commands.Process;
import edgruberman.bukkit.benefits.commands.Register;
import edgruberman.bukkit.benefits.commands.Reload;
import edgruberman.bukkit.benefits.commands.Sandbox;
import edgruberman.bukkit.benefits.commands.Undo;
import edgruberman.bukkit.benefits.messaging.Courier.ConfigurationCourier;
import edgruberman.bukkit.benefits.util.BufferedYamlConfiguration;
import edgruberman.bukkit.benefits.util.CustomPlugin;

public final class Main extends CustomPlugin {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz");
    private static final String PACKAGES_FILE = "packages.yml";
    private static final String LANGUAGE_FILE = "language.yml";

    public static ConfigurationCourier courier;

    private Coordinator coordinator;
    private BufferedYamlConfiguration contributions;
    private BufferedYamlConfiguration pending;
    private BufferedYamlConfiguration registrations;
    private final List<Processor> processors = new ArrayList<Processor>();

    @Override
    public void onLoad() {
        this.putConfigMinimum("0.0.0a109");
        this.putConfigMinimum(Main.LANGUAGE_FILE, "0.0.0b37");
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

        this.contributions = new BufferedYamlConfiguration(this, new File(this.getDataFolder(), "contributions.yml"), 5000);
        try { this.contributions.load(); } catch (final Exception e) { throw new IllegalStateException("Unable to load contributions.yml file; {0}", e); }
        this.loadContributions(this.contributions);
        this.getLogger().log(Level.CONFIG, "Loaded {0} contribution{0,choice,0#s|1#|2#s}", this.coordinator.contributions.size());

        this.pending = new BufferedYamlConfiguration(this, new File(this.getDataFolder(), "pending.yml"), 5000);
        try { this.pending.load(); } catch (final Exception e) { throw new IllegalStateException("Unable to load pending.yml file; {0}", e); }
        this.loadPending(this.pending);

        this.registrations = new BufferedYamlConfiguration(this, new File(this.getDataFolder(), "registrations.yml"), 5000);
        try { this.registrations.load(); } catch (final Exception e) { throw new IllegalStateException("Unable to load registrations.yml file; {0}", e); }
        this.loadRegistrations(this.registrations);
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
        this.getCommand("benefits:contributions").setExecutor(new Contributions(this.coordinator));
        this.getCommand("benefits:benefits").setExecutor(new Benefits(this.coordinator));
        this.getCommand("benefits:process").setExecutor(new Process(this.coordinator));
        this.getCommand("benefits:assign").setExecutor(new Assign(this.coordinator));
        this.getCommand("benefits:undo").setExecutor(new Undo(this.coordinator));
        this.getCommand("benefits:sandbox").setExecutor(new Sandbox(this.coordinator));
        this.getCommand("benefits:register").setExecutor(new Register(this.coordinator));
        this.getCommand("benefits:reload").setExecutor(new Reload(this));
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

    private void loadContributions(final ConfigurationSection contributions) {
        final String lower = this.coordinator.currency.toLowerCase();
        int mismatched = 0;

        for (final String key : contributions.getKeys(false)) {
            final ConfigurationSection entry = contributions.getConfigurationSection(key);

            final String currency = entry.getString("currency");
            if (!lower.equals(currency.toLowerCase())) {
                mismatched++;
                continue;
            }

            long contributed;
            try {
                contributed = Main.DATE_FORMAT.parse(entry.getString("contributed")).getTime();
            } catch (final ParseException e) {
                throw new IllegalStateException(e);
            }

            final Contribution contribution = new Contribution(entry.getString("processor"), entry.getString("id"), entry.getString("origin")
                    , entry.getString("player"), currency, entry.getLong("amount"), contributed, ( entry.isSet("packages") ? entry.getStringList("packages") : null ));

            final Contribution duplicate = this.coordinator.putContribution(contribution);
            if (duplicate != null) this.getLogger().log(Level.WARNING, "Unable to load duplicate contribution from contributions.yml; {0}", duplicate);
        }

        if (mismatched > 0) this.getLogger().log(Level.WARNING, "Unable to load {0} currency mismatched contributions from contributions.yml; Required currency: {1}"
                , new Object[] { mismatched, this.coordinator.currency });
    }

    void saveContribution(final Contribution contribution) {
        final ConfigurationSection entry = this.contributions.createSection(contribution.getKey());
        entry.set("processor", contribution.processor);
        entry.set("id", contribution.id);
        entry.set("origin", contribution.origin);
        entry.set("player", contribution.player);
        entry.set("currency", contribution.currency);
        entry.set("amount", contribution.amount);
        entry.set("contributed", Main.DATE_FORMAT.format(new Date(contribution.contributed)));
        entry.set("packages", contribution.packages);
        this.contributions.queueSave();
    }

    private void loadPending(final ConfigurationSection pending) {
        for (final String packageName : pending.getKeys(false)) {
            final ConfigurationSection benefits = pending.getConfigurationSection(packageName);

            for (final String benefitName : benefits.getKeys(false)) {
                final ConfigurationSection commands = benefits.getConfigurationSection(benefitName);

                for (final String commandName : commands.getKeys(false)) {

                    for (final String key : commands.getStringList(commandName)) {
                        final Contribution contribution = this.coordinator.getContribution(key);
                        if (contribution == null) {
                            this.getLogger().warning("Unable to find contribution " + key + " to add to " + packageName + "." + benefitName + "." + commandName);
                            continue;
                        }

                        final Package pkg = this.coordinator.getPackage(packageName.toLowerCase());
                        if (pkg == null) {
                            this.getLogger().warning("Unable to find package " + packageName + " to add " + key + " to " + packageName + "." + benefitName + "." + commandName);
                            continue;
                        }

                        final Benefit benefit = pkg.benefits.get(benefitName.toLowerCase());
                        if (benefit == null) {
                            this.getLogger().warning("Unable to find benefit " + benefitName + " to add " + key + " to " + packageName + "." + benefitName + "." + commandName);
                            continue;
                        }

                        final Command command = benefit.commands.get(commandName.toLowerCase());
                        if (command == null) {
                            this.getLogger().warning("Unable to find command " + commandName + " to add " + key + " to " + packageName + "." + benefitName + "." + commandName);
                            continue;
                        }

                        command.assign(contribution);
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
                        final List<String> contributions = new ArrayList<String>();
                        for (final Contribution contribution : trigger.getPending()) contributions.add(contribution.getKey());
                        if (contributions.size() > 0) this.pending.set(pkg.name + "." + benefit.name + "." + command.name, contributions);
                    }
                }
            }
        }

        this.pending.queueSave();
    }

    private void loadRegistrations(final ConfigurationSection registrations) {
        for (final String player : registrations.getKeys(false)) {
            for (final String origin : registrations.getStringList(player)) {
                this.coordinator.registrations.put(origin, player);
            }
        }
    }

    void saveRegistration(final String origin, final String player) {
        final List<String> origins = this.registrations.getStringList(player);
        origins.add(origin);
        this.registrations.set(player, origins);
        this.registrations.queueSave();
    }

    void deleteRegistration(final String origin, final String player) {
        final List<String> origins = this.registrations.getStringList(player);
        if (!origins.remove(origin)) return;
        this.registrations.set(player, ( origins.size() >= 1 ? origins : null ));
        this.registrations.queueSave();
    }

}
