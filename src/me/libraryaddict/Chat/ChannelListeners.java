package me.libraryaddict.Chat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;

public class ChannelListeners implements Listener, PluginMessageListener {

    private Main main;

    public ChannelListeners(Main main) {
        this.main = main;
    }

    private void chatChannel(ChatChannel channel, Player player, String format, String message, String shortcutMessage) {
        String sender = (channel.useDisplayNames() ? player.getDisplayName() : player.getName());
        format = (channel.getFormat() != null ? channel.getFormat() : format);
        if (channel.isCrossServer()) {
            sendData(channel.getName(), sender, format, message);
        }
        message = String.format(format, sender, message);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (channel.getRadius() < 0 || p.getLocation().distance(player.getLocation()) <= channel.getRadius()) {
                if (main.getChatChannel(p) == channel
                        || (channel.useHearPermission() && p.hasPermission(channel.getPermissionToHear()))) {
                    p.sendMessage(message);
                } else {
                    if (p == player && shortcutMessage != null) {
                        p.sendMessage(shortcutMessage);
                    }
                }
            }
        }
        System.out.print(message);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChat(PlayerChatEvent event) {
        if (event.isCancelled() || event.getRecipients().isEmpty())
            return;
        ChatChannel channel = main.getChatChannel(event.getPlayer());
        Main.ChannelShortcut shortcut = main.getShortcut(event.getMessage());
        if (shortcut != null) {
            if (shortcut.getPermission() != null && !event.getPlayer().hasPermission(shortcut.getPermission())) {
                event.getPlayer().sendMessage(ChatColor.RED + "You do not have access to this channel!");
                event.setCancelled(true);
                return;
            }
            channel = shortcut.getChannel();
            event.setMessage(event.getMessage().substring(shortcut.getKey().length()).trim());
        }
        if (channel != null) {
            event.setCancelled(true);
            if (event.getMessage().trim().length() <= 0) {
                event.getPlayer().sendMessage(ChatColor.RED + "You cannot send a empty message");
                return;
            }
            chatChannel(channel, event.getPlayer(), event.getFormat(), event.getMessage(),
                    shortcut != null && shortcut.getChannel() == channel ? shortcut.getMessage() : null);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Main.ChannelShortcut shortcut = main.getShortcut(event.getMessage());
        if (shortcut != null && !event.isCancelled()) {
            event.setCancelled(true);
            if (shortcut.getPermission() != null && !event.getPlayer().hasPermission(shortcut.getPermission())) {
                event.getPlayer().sendMessage(ChatColor.RED + "You do not have access to this channel!");
                return;
            }
            String msg = event.getMessage().substring(shortcut.getKey().length());
            if (msg.length() == 0) {
                event.getPlayer().sendMessage(ChatColor.RED + "You cannot send a empty message!");
                return;
            }
            PlayerChatEvent chatEvent = new PlayerChatEvent(event.getPlayer(), msg, "<%s> %s", new HashSet());
            Bukkit.getPluginManager().callEvent(chatEvent);
            if (!chatEvent.isCancelled()) {
                chatChannel(shortcut.getChannel(), event.getPlayer(), "<%s> %s", chatEvent.getMessage(), shortcut.getMessage());
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (main.getDefaultChannel() != null) {
            main.addToChannel(main.getDefaultChannel(), event.getPlayer());
        }
    }

    @Override
    public void onPluginMessageReceived(String pluginChannel, Player whoGivesAShitAboutThePlayer, byte[] bytes) {
        if (!pluginChannel.equals("BungeeCord")) {
            return;
            // This is not the channel we are looking for..
        }
        try {

            DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
            String subchannel = in.readUTF();
            short len = in.readShort();
            byte[] msgbytes = new byte[len];
            in.readFully(msgbytes);
            DataInputStream msgin = new DataInputStream(new ByteArrayInputStream(msgbytes));
            if (subchannel.equals("LibrarysChat")) {
                long timestamp = msgin.readLong();
                if (timestamp >= System.currentTimeMillis()) {
                    String channelName = msgin.readUTF();
                    String sender = msgin.readUTF();
                    String format = msgin.readUTF();
                    String message = msgin.readUTF();
                    for (ChatChannel channel : main.getChannels()) {
                        if (channel.getName().equals(channelName) && channel.isCrossServer()) {
                            String chatMessage = String.format(format, sender, message);
                            for (Player player : Bukkit.getOnlinePlayers()) {
                                if (main.getChatChannel(player) == channel
                                        || (channel.useHearPermission() && player.hasPermission(channel.getPermissionToHear()))) {
                                    player.sendMessage(chatMessage);
                                }
                            }
                            break;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        ChatChannel hisChannel = main.getChatChannel(event.getPlayer());
        if (hisChannel != null) {
            main.removeFromChannel(hisChannel, event.getPlayer());
        }
        main.getChatManager().removeChatter(event.getPlayer().getName());
    }

    private void sendData(String channel, String sender, String format, String message) {
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(b);
            out.writeUTF("Forward");
            out.writeUTF("ALL");
            out.writeUTF("LibrarysChat");

            ByteArrayOutputStream msgbytes = new ByteArrayOutputStream();
            DataOutputStream msgout = new DataOutputStream(msgbytes);
            msgout.writeLong(System.currentTimeMillis() + 1000);
            msgout.writeUTF(channel);
            msgout.writeUTF(sender);
            msgout.writeUTF(format);
            msgout.writeUTF(message);
            out.writeShort(msgbytes.toByteArray().length);
            out.write(msgbytes.toByteArray());
            Bukkit.getOnlinePlayers()[0].sendPluginMessage(main, "BungeeCord", b.toByteArray());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
