package edgruberman.bukkit.benefits.commands;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import edgruberman.bukkit.benefits.Contribution;
import edgruberman.bukkit.benefits.Coordinator;
import edgruberman.bukkit.benefits.Main;
import edgruberman.bukkit.benefits.Package;
import edgruberman.bukkit.benefits.util.JoinList;

public final class Contributions implements CommandExecutor {

    private final Coordinator coordinator;
    private final int pageSize = 4;

    public Contributions(final Coordinator coordinator) {
        this.coordinator = coordinator;
    }

    // 2008 Aug 01 06:11PM (1132d) = $0.99 USD
    // 2009 Aug 07 09:12PM (751d) = $0.99 USD
    // 2010 Aug 22 11:14PM (186d) = $0.99 USD
    // 2012 Jun 01 06:30AM (2d) = $15.00 USD [Contributor]
    // First 1132 days | Last 2 days | Page 3 of 3
    // usage: /<command>[ <Page>][ <Player>]
    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        final Integer page =  Contributions.parsePage(args);

        if (args.length < 2 && !(sender instanceof Player)) {
            Main.courier.send(sender, "requires-argument", "page contribution", 0);
            return false;
        }

        final String target = (args.length >= 2 ? Bukkit.getOfflinePlayer(args[1]).getName() : sender.getName());
        final List<Contribution> history = this.coordinator.history(target);
        if (history.size() == 0) {
            Main.courier.send(sender, "history.none", target);
            return true;
        }

        final int total = (int) Math.ceil(history.size() / (double) this.pageSize);
        final int first = Math.min((page - 1) * this.pageSize, history.size() - 1);
        final int last = Math.min(first + this.pageSize, history.size());
        final long now = System.currentTimeMillis();
        for (final Contribution contribution : history.subList(first, last)) {
            final List<String> packages = new JoinList<String>(Main.courier.getSection("history.packages"));
            for (final String name : contribution.packages) {
                final Package pkg = this.coordinator.getPackage(name);
                if (pkg != null && !pkg.visible()) continue;
                packages.add(name);
            }

            final long days = TimeUnit.MILLISECONDS.toDays(now - contribution.contributed);
            Main.courier.send(sender, "history.contribution", new Date(contribution.contributed), days, contribution.currency, contribution.amount / 100D, packages);
        }

        final long oldest = TimeUnit.MILLISECONDS.toDays(now - history.get(history.size() - 1).contributed);
        final long newest = TimeUnit.MILLISECONDS.toDays(now - history.get(0).contributed);
        long sum = 0; for (final Contribution contribution : history) sum += contribution.amount;
        Main.courier.send(sender, "history.summary", target, oldest, newest, page, total, history.size(), this.coordinator.currency, sum / 100D);
        return true;
    }

    private static int parsePage(final String[] args) {
        if (args.length == 0) return 1;

        try { return Math.max(Integer.parseInt(args[0]), 1); }
        catch(final Exception e) { return 1; }
    }

}
