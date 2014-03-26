package me.libraryaddict.Chat.Commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class CommandAlias implements CommandExecutor {
    private String command;

    public CommandAlias(String command) {
        this.command = command;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        Bukkit.dispatchCommand(sender, command);
        return true;
    }
}
