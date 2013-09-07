package edgruberman.bukkit.benefits.commands;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import edgruberman.bukkit.benefits.Contribution;
import edgruberman.bukkit.benefits.Coordinator;
import edgruberman.bukkit.benefits.Main;
import edgruberman.bukkit.benefits.processors.SlashCommand;
import edgruberman.bukkit.benefits.util.JoinList;

public final class Process implements CommandExecutor {

    SlashCommand processor;

    public Process(final Coordinator coordinator) {
        this.processor = new SlashCommand(coordinator, null);
    }

    // usage: /<command> <Contributor> <Amount>[ <When>]
    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (args.length < 1) {
            Main.courier.send(sender, "requires-argument", "contributor", 0);
            return false;
        }

        if (args.length < 2) {
            Main.courier.send(sender, "requires-argument", "amount", 0);
            return false;
        }

        final String contributor = args[0];
        final Double amount = Process.parseDouble(args[1]);
        if (amount == null) {
            Main.courier.send(sender, "unknown-argument", "amount", 0, args[1]);
            return false;
        }
        final long converted = (long) (amount * 100);

        final Date when = (args.length >= 3 ? Process.parseDate(args[2]) : new Date());
        if (when == null) {
            Main.courier.send(sender, "unknown-argument", "when", 0, args[2]);
            return false;
        }

        final Contribution contribution = this.processor.process(sender.getName(), contributor, converted, when.getTime());
        final List<String> packages = new JoinList<String>(Main.courier.getSection("process.packages"), contribution.packages);
        Main.courier.send(sender, "process.success", contributor, contribution.currency, converted / 100D, packages);
        return true;
    }

    private static Double parseDouble(final String s) {
        try { return Double.parseDouble(s);
        } catch(final Exception e) { return null; }
    }

    private static Date parseDate(final String s) {
        try { return (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz")).parse(s);
        } catch (final ParseException e) { return null; }
    }

}
