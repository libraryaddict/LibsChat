package me.libraryaddict.Chat.Commands;

import me.libraryaddict.Chat.ChatManager;

import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class Message implements CommandExecutor {
    private ChatManager chatManager;
    private String commandName;

    public Message(ChatManager manager, String command) {
        commandName = command;
        chatManager = manager;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (args.length > 1) {
            chatManager.sendMessage(sender, args[0], StringUtils.join(args, " ").substring(args[0].length() + 1));
        } else
            sender.sendMessage(ChatColor.RED + "/" + commandName + " <Player> <Message>");
        return true;
    }
}
