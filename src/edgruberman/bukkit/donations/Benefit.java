package edgruberman.bukkit.donations;

import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;

/** collection of commands */
public final class Benefit {

    public final Package pkg;
    public final String name;
    public final String description;
    public final Integer limit;
    public final Map<String, Command> commands = new LinkedHashMap<String, Command>();

    Benefit(final Package pkg, final ConfigurationSection definition) {
        this.pkg = pkg;
        this.name = definition.getName();
        this.description = definition.getString("description");
        this.limit = definition.getInt("limit");

        final ConfigurationSection commands = definition.getConfigurationSection("commands");
        if (commands == null) return;
        for (final String commandName : commands.getKeys(false)) {
            final Command command = new Command(this, commands.getConfigurationSection(commandName));
            this.commands.put(command.name.toLowerCase(), command);
        }
    }

    void clear() {
        for (final Command command : this.commands.values()) command.clear();
        this.commands.clear();
    }

    public String getPath() {
        return this.pkg.getPath() + ">\"" + this.name + "\"";
    }

}
