package me.libraryaddict.Chat.Commands;

import java.util.ArrayList;
import java.util.Collections;

import me.libraryaddict.Chat.ChatChannel;
import me.libraryaddict.Chat.Main;

import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ChannelCommand implements CommandExecutor {
    private String commandName;
    private Main main;

    public ChannelCommand(Main main, String commandName) {
        this.main = main;
        this.commandName = commandName;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (sender.getName().equals("CONSOLE") && !(args.length == 0 || args[0].equalsIgnoreCase("reload"))) {
            sender.sendMessage(ChatColor.RED + "Console cannot do this!");
            sender.sendMessage(ChatColor.RED + "Console may only use /channel reload");
            return true;
        }
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("join")) {
                if (args.length > 1) {
                    ChatChannel joining = null;
                    ChatChannel leaving = main.getChatChannel((Player) sender);
                    for (ChatChannel channel : main.getChannels()) {
                        if (channel.getName().equalsIgnoreCase(args[1]))
                            joining = channel;
                    }
                    if (joining == null) {
                        sender.sendMessage(ChatColor.RED + "Cannot find channel " + ChatColor.GREEN + args[1]);
                    } else if (joining == leaving) {
                        sender.sendMessage(ChatColor.RED + "Is this some kind of joke? You are already in that channel");
                    } else if (leaving != null && leaving.useLeavePermission()
                            && !sender.hasPermission(leaving.getPermissionToLeave())) {
                        sender.sendMessage(ChatColor.RED + "You don't have permission to leave your current channel!");
                    } else if (joining.useJoinPermission() && !sender.hasPermission(joining.getPermissionToJoin())) {
                        sender.sendMessage(ChatColor.RED + "You don't have permission to join this channel!");
                    } else {
                        if (leaving != null)
                            main.removeFromChannel(leaving, (Player) sender);
                        main.addToChannel(joining, (Player) sender);
                        sender.sendMessage(ChatColor.RED + "Joined channel " + joining.getName());
                    }
                } else
                    sender.sendMessage(ChatColor.RED + "Please state a channel name!");
            } else if (args[0].equalsIgnoreCase("leave")) {
                ChatChannel leaving = main.getChatChannel((Player) sender);
                if (leaving == null) {
                    sender.sendMessage(ChatColor.RED + "Is this some kind of joke? You are not in a channel");
                } else if (leaving.useLeavePermission() && !sender.hasPermission(leaving.getPermissionToLeave())) {
                    sender.sendMessage(ChatColor.RED + "You don't have permission to leave this channel!");
                } else {
                    main.removeFromChannel(leaving, (Player) sender);
                    sender.sendMessage(ChatColor.RED + "Left channel " + leaving.getName());
                }
            } else if (args[0].equalsIgnoreCase("list")) {
                ArrayList<String> channelNames = new ArrayList<String>();
                for (ChatChannel channel : main.getChannels()) {
                    if (!channel.useJoinPermission() || sender.hasPermission(channel.getPermissionToJoin()))
                        channelNames.add(channel.getName());
                }
                Collections.sort(channelNames, String.CASE_INSENSITIVE_ORDER);
                sender.sendMessage(ChatColor.DARK_RED + "Channels: " + ChatColor.RED
                        + StringUtils.join(channelNames, ChatColor.DARK_RED + ", " + ChatColor.RED));
            } else if (args[0].equalsIgnoreCase("reload") && sender.hasPermission("libraryschat.reloadconfig")) {
                main.loadConfig();
                sender.sendMessage(ChatColor.RED + "Reloaded channels config");
            } else
                sendUsage(sender);
        } else
            sendUsage(sender);
        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "/" + commandName + " join <Channel>");
        sender.sendMessage(ChatColor.RED + "/" + commandName + " leave");
        sender.sendMessage(ChatColor.RED + "/" + commandName + " list");
        if (sender.hasPermission("libraryschat.reloadconfig"))
            sender.sendMessage(ChatColor.RED + "/" + commandName + " reload");
    }
}