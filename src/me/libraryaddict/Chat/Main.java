package me.libraryaddict.Chat;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import me.libraryaddict.Chat.Commands.*;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    public class ChannelShortcut {
        private ChatChannel channel;
        private String key;
        private String permission;
        private String message;

        public ChannelShortcut(ChatChannel channel, String key, String perm, String message) {
            this.channel = channel;
            if (message != null)
                message = ChatColor.translateAlternateColorCodes('&', message);
            this.message = message;
            this.key = key;
            permission = perm;
        }

        public String getMessage() {
            return message;
        }

        public ChatChannel getChannel() {
            return channel;
        }

        public String getKey() {
            return key;
        }

        public String getPermission() {
            return permission;
        }
    }

    private ArrayList<ChatChannel> channels = new ArrayList<ChatChannel>();
    private SimpleCommandMap commandMap;
    private ChatChannel defaultChannel = null;
    private HashMap<ChatChannel, List<Player>> players = new HashMap<ChatChannel, List<Player>>();
    private ChatManager chatManager = new ChatManager(this);

    public ChatManager getChatManager() {
        return chatManager;
    }

    private HashMap<String, ChannelShortcut> shortcuts = new HashMap<String, ChannelShortcut>();

    public void addToChannel(ChatChannel channel, Player player) {
        if (!players.containsKey(channel))
            players.put(channel, new ArrayList<Player>());
        players.get(channel).add(player);
    }

    public ArrayList<ChatChannel> getChannels() {
        return channels;
    }

    public ChatChannel getChatChannel(Player player) {
        for (ChatChannel channel : players.keySet())
            if (players.get(channel).contains(player))
                return channel;
        return null;
    }

    public ChatChannel getDefaultChannel() {
        return defaultChannel;
    }

    public ChannelShortcut getShortcut(String message) {
        for (String key : shortcuts.keySet()) {
            if (message.length() >= key.length() && message.split(" ")[0].equalsIgnoreCase(key)) {
                return shortcuts.get(key);
            }
        }
        return null;
    }

    public void loadConfig() {
        ArrayList<ChatChannel> newChannels = new ArrayList<ChatChannel>();
        saveDefaultConfig();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "config.yml"));
        for (String key : config.getConfigurationSection("Channels").getKeys(false)) {
            newChannels.add(new ChatChannel(config.getConfigurationSection("Channels." + key)));
        }
        defaultChannel = null;
        if (config.contains("DefaultChannel")) {
            for (ChatChannel channel : newChannels) {
                if (channel.getName().equals(config.getString("DefaultChannel")))
                    defaultChannel = channel;
            }
        }
        for (ChatChannel channel : channels) {
            if (!newChannels.contains(channel)) {
                if (players.containsKey(channel))
                    players.remove(channel);
            }
        }
        channels = newChannels;
        if (config.contains("BindCommands"))
            for (String key : config.getConfigurationSection("BindCommands").getKeys(false)) {
                registerCommand(key, new CommandAlias(config.getString("BindCommands." + key)), new ArrayList<String>());
            }
        shortcuts.clear();
        if (config.contains("ChannelShortcuts"))
            for (String key : config.getConfigurationSection("ChannelShortcuts").getKeys(false)) {
                String channelName = config.getString("ChannelShortcuts." + key + ".ChannelName");
                for (ChatChannel channel : getChannels()) {
                    if (channel.getName().equalsIgnoreCase(channelName)) {
                        shortcuts.put(key,
                                new ChannelShortcut(channel, key, config.getString("ChannelShortcuts." + key + ".Permission"),
                                        config.getString("ChannelShortcuts." + key + ".Message")));
                    }
                }
            }
    }

    public void onEnable() {
        try {
            commandMap = (SimpleCommandMap) Bukkit.getServer().getClass().getDeclaredMethod("getCommandMap")
                    .invoke(Bukkit.getServer());
        } catch (Exception e) {
            e.printStackTrace();
        }
        ChannelListeners listeners = new ChannelListeners(this);
        Bukkit.getPluginManager().registerEvents(listeners, this);
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        Bukkit.getMessenger().registerIncomingPluginChannel(this, "BungeeCord", listeners);
        Bukkit.getMessenger().registerIncomingPluginChannel(this, "BungeeCord", chatManager);
        loadConfig();
        final Main main = this;
        Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
            public void run() {
                YamlConfiguration baseConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "config.yml"));
                if (baseConfig.contains("Commands")) {
                    for (String key : baseConfig.getConfigurationSection("Commands").getKeys(false)) {
                        ConfigurationSection config = baseConfig.getConfigurationSection("Commands." + key);

                        String commandName = config.getString("Command");
                        if (commandName == null)
                            commandName = config.getName();
                        commandName = commandName.toLowerCase();
                        List<String> aliases = new ArrayList<String>();
                        if (config.contains("Aliases"))
                            aliases = config.getStringList("Aliases");
                        if (key.equals("Channel")) {
                            registerCommand(commandName, new ChannelCommand(main, commandName), aliases);
                        } else if (key.equals("Message")) {
                            registerCommand(commandName, new Message(chatManager, commandName), aliases);
                        } else if (key.equals("Reply")) {
                            registerCommand(commandName, new Reply(chatManager), aliases);
                        }
                    }
                    if (Bukkit.getPluginManager().getPermission("ThisIsUsedForMessaging") == null) {
                        Permission perm = new Permission("ThisIsUsedForMessaging", PermissionDefault.TRUE);
                        perm.setDescription("Used for messages in LibsHungergames");
                        Bukkit.getPluginManager().addPermission(perm);
                    }
                } else {
                    getLogger()
                            .log(Level.SEVERE,
                                    "You need to regenerate LibsChat config! Its missing options and I don't think you would like it if I regenerated it!");
                }
            }
        }, 5);

    }

    private void registerCommand(String name, CommandExecutor exc, List<String> aliases) {
        try {
            PluginCommand command = Bukkit.getServer().getPluginCommand(name.toLowerCase());
            if (command == null) {
                Constructor<?> constructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
                constructor.setAccessible(true);
                command = (PluginCommand) constructor.newInstance(name, this);
            }
            command.setExecutor(exc);
            command.setAliases(aliases);
            for (String alias : command.getAliases())
                unregisterCommand(alias);
            commandMap.register(name, command);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void removeFromChannel(ChatChannel channel, Player player) {
        if (players.containsKey(channel)) {
            players.get(channel).remove(player);
            if (players.get(channel).size() == 0)
                players.remove(channel);
        }
    }

    private void unregisterCommand(String name) {
        try {
            Field known = SimpleCommandMap.class.getDeclaredField("knownCommands");
            Field alias = SimpleCommandMap.class.getDeclaredField("aliases");
            known.setAccessible(true);
            alias.setAccessible(true);
            Map<String, Command> knownCommands = (Map<String, Command>) known.get(commandMap);
            Set<String> aliases = (Set<String>) alias.get(commandMap);
            knownCommands.remove(name.toLowerCase());
            aliases.remove(name.toLowerCase());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
