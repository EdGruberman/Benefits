package edgruberman.bukkit.donations;

import java.util.LinkedHashMap;
import java.util.Map;

/** Collection of commands */
public final class Benefit {

    public final Package pkg;
    public final String name;
    public final String description;
    public final Integer limit;
    public final Map<String, Command> commands = new LinkedHashMap<String, Command>();

    Benefit(final Package pkg, final String name, final String description, final Integer limit) {
        this.pkg = pkg;
        this.name = name;
        this.description = description;
        this.limit = limit;
    }

    void clear() {
        for (final Command command : this.commands.values()) command.clear();
        this.commands.clear();
    }

    public String getPath() {
        return this.pkg.getPath() + ".\"" + this.name + "\"";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.name == null) ? 0 : this.name.hashCode());
        return result;
    }
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        final Benefit other = (Benefit) obj;
        if (this.name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!this.name.equals(other.name)) {
            return false;
        }
        return true;
    }

}