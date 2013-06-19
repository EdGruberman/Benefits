package edgruberman.bukkit.donations;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;

/** collection of commands */
public final class Benefit {

    public final Package pkg;
    public final String name;
    public final String description;
    public final Map<String, Command> commands = new LinkedHashMap<String, Command>();

    Benefit(final Package pkg, final ConfigurationSection definition) {
        this.pkg = pkg;
        this.name = definition.getName();
        this.description = definition.getString("description");

        final ConfigurationSection commands = definition.getConfigurationSection("commands");
        if (commands != null) {
            for (final String commandName : commands.getKeys(false)) {
                final Command command = new Command(this, commands.getConfigurationSection(commandName));
                this.commands.put(command.name.toLowerCase(), command);
            }
        }

        final ConfigurationSection expiration = definition.getConfigurationSection("expiration");
        if (expiration != null) {
            final Command command = new Expiration(this, expiration);
            this.commands.put(command.name.toLowerCase(), command);
        }
    }

    void clear() {
        for (final Command command : this.commands.values()) command.clear();
        this.commands.clear();
    }

    public List<Command> assign(final Donation donation) {
        final List<Command> result = new ArrayList<Command>();
        for (final Command command : this.commands.values()) {
            command.assign(donation);
            result.add(command);
        }
        return result;
    }

    public String getPath() {
        return this.pkg.getPath() + ">\"" + this.name + "\"";
    }

}
