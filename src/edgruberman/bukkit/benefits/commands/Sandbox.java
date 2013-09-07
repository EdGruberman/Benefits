package edgruberman.bukkit.benefits.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import edgruberman.bukkit.benefits.Coordinator;
import edgruberman.bukkit.benefits.Main;

public final class Sandbox implements CommandExecutor {

    private final Coordinator coordinator;

    public Sandbox(final Coordinator coordinator) {
        this.coordinator = coordinator;
    }

    // usage: '§f-> §7Usage: §b§l/<command> §3[§lon§3|§loff§3]'
    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        final boolean enable = ( args.length < 1 ? !this.coordinator.isSandbox() : args[0].equals("on"));
        this.coordinator.setSandbox(enable);
        Main.courier.send(sender, "sandbox", this.coordinator.isSandbox()?1:0);
        return true;
    }

}
