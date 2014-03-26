package me.libraryaddict.Chat.Commands;

import me.libraryaddict.Chat.ChatManager;

import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class Reply implements CommandExecutor {
    private ChatManager manager;

    public Reply(ChatManager manager) {
        this.manager = manager;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (manager.hasOtherChatter(sender.getName())) {
            if (args.length == 0) {
                sender.sendMessage(ChatColor.RED + "You need to type a reply!");
                return true;
            }
            manager.sendMessage(sender, manager.getOtherChatter(sender.getName()), StringUtils.join(args, " "));
        } else
            sender.sendMessage(ChatColor.RED + "Nobody to reply to");
        return true;
    }
}
