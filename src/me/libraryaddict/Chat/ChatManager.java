package me.libraryaddict.Chat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.HashMap;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitRunnable;

public class ChatManager implements PluginMessageListener {

    private HashMap<String, String> lastMsg = new HashMap<String, String>();
    private HashMap<CommandSender, BukkitRunnable> noReplies = new HashMap<CommandSender, BukkitRunnable>();
    private Main main;

    public ChatManager(Main main) {
        this.main = main;
    }

    public String getOtherChatter(String player) {
        if (lastMsg.containsKey(player))
            return lastMsg.get(player);
        return null;
    }

    public CommandSender getSender(String name, boolean startsWith) {
        Set<Permissible> permissibles = Bukkit.getPluginManager().getPermissionSubscriptions("ThisIsUsedForMessaging");
        for (Permissible permissible : permissibles) {
            if (permissible instanceof CommandSender) {
                CommandSender user = (CommandSender) permissible;
                if (user.getName().equalsIgnoreCase(name)
                        || (startsWith && user.getName().toLowerCase().startsWith(name.toLowerCase())))
                    return user;
            }
        }
        return null;
    }

    public boolean hasOtherChatter(String player) {
        return lastMsg.containsKey(player);
    }

    public void removeChatter(String chatter) {
        lastMsg.remove(chatter);
    }

    private void sendData(String sender, String receiver, String toSender, String toReceiver) {
        try {
            ByteArrayOutputStream msgbytes = new ByteArrayOutputStream();
            DataOutputStream msgout = new DataOutputStream(msgbytes);
            msgout.writeLong(System.currentTimeMillis() + 1000);
            msgout.writeBoolean(true);
            msgout.writeUTF(sender);
            msgout.writeUTF(receiver);
            msgout.writeUTF(toSender);
            msgout.writeUTF(toReceiver);
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(b);

            out.writeUTF("Forward");
            out.writeUTF("ALL");
            out.writeUTF("LibrarysMessage");
            out.writeShort(msgbytes.toByteArray().length);
            out.write(msgbytes.toByteArray());
            Bukkit.getOnlinePlayers()[0].sendPluginMessage(main, "BungeeCord", b.toByteArray());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void sendConfirmation(String sender, String receiver, String message) {
        try {
            ByteArrayOutputStream msgbytes = new ByteArrayOutputStream();
            DataOutputStream msgout = new DataOutputStream(msgbytes);
            msgout.writeLong(System.currentTimeMillis() + 1000);
            msgout.writeBoolean(false);
            msgout.writeUTF(sender);
            msgout.writeUTF(receiver);
            msgout.writeUTF(message);
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(b);

            out.writeUTF("Forward");
            out.writeUTF("ALL");
            out.writeUTF("LibrarysMessage");
            out.writeShort(msgbytes.toByteArray().length);
            out.write(msgbytes.toByteArray());
            Bukkit.getOnlinePlayers()[0].sendPluginMessage(main, "BungeeCord", b.toByteArray());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void sendMessage(final CommandSender sender, final String receiver, String message) {
        if (noReplies.containsKey(sender.getName())) {
            sender.sendMessage(ChatColor.RED + "Stop spamming messages!");
            return;
        }
        CommandSender otherGuy = getSender(receiver, true);
        String name = sender instanceof Player ? ((Player) sender).getDisplayName() : sender.getName();
        String toSender = ChatColor.GRAY + "[me -> %s" + ChatColor.RESET + ChatColor.GRAY + "] " + ChatColor.RESET + message;
        String toReceiver = ChatColor.GRAY + "[" + name + ChatColor.RESET + ChatColor.GRAY + " -> me] " + ChatColor.RESET
                + message;
        if (otherGuy == null) {
            BukkitRunnable runnable = new BukkitRunnable() {
                public void run() {
                    noReplies.remove(sender);
                    sender.sendMessage(ChatColor.RED + "Cannot find player '" + receiver + "'!");
                }
            };
            noReplies.put(sender, runnable);
            runnable.runTaskLater(main, 10);
            sendData(sender.getName(), receiver, toSender, toReceiver);
        } else {
            String receiverName = otherGuy instanceof Player ? ((Player) otherGuy).getDisplayName() : otherGuy.getName();
            sender.sendMessage(toSender.replaceFirst("%s", receiverName));
            otherGuy.sendMessage(toReceiver.replaceFirst("%s", receiverName));
            this.lastMsg.put(sender.getName(), otherGuy.getName());
            this.lastMsg.put(otherGuy.getName(), sender.getName());
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
            if (subchannel.equals("LibrarysMessage")) {
                long timestamp = msgin.readLong();
                if (timestamp >= System.currentTimeMillis()) {
                    boolean isMessage = msgin.readBoolean();
                    String sender = msgin.readUTF();
                    String receiver = msgin.readUTF();
                    String message = msgin.readUTF();
                    CommandSender player = getSender(isMessage ? receiver : sender, false);
                    if (player != null) {
                        lastMsg.put(player.getName(), isMessage ? sender : receiver);
                        if (isMessage) {
                            player.sendMessage(msgin.readUTF());
                            sendConfirmation(sender, player.getName(), message.replaceFirst("%s",
                                    player instanceof Player ? ((Player) player).getDisplayName() : player.getName()));
                        } else {
                            // Confirmed the message has been sent
                            if (noReplies.containsKey(player))
                                noReplies.remove(player).cancel();
                            player.sendMessage(message);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
