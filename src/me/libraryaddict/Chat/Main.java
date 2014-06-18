package me.libraryaddict.Chat;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import me.libraryaddict.Chat.Commands.*;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.PluginCommandYamlParser;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    public class ChannelShortcut {
        private ChatChannel channel;
        private String key;
        private String message;
        private String permission;

        public ChannelShortcut(ChatChannel channel, String key, String perm, String message) {
            this.channel = channel;
            if (message != null)
                message = ChatColor.translateAlternateColorCodes('&', message);
            this.message = message;
            this.key = key;
            permission = perm;
        }

        public ChatChannel getChannel() {
            return channel;
        }

        public String getKey() {
            return key;
        }

        public String getMessage() {
            return message;
        }

        public String getPermission() {
            return permission;
        }
    }

    private ArrayList<ChatChannel> channels = new ArrayList<ChatChannel>();
    private ChatManager chatManager = new ChatManager(this);
    private Map<String, Map<String, Object>> commandsMap = new HashMap<String, Map<String, Object>>();
    private ChatChannel defaultChannel = null;
    private HashMap<ChatChannel, List<Player>> players = new HashMap<ChatChannel, List<Player>>();
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

    public ChatManager getChatManager() {
        return chatManager;
    }

    public ChatChannel getDefaultChannel() {
        return defaultChannel;
    }

    public ChannelShortcut getShortcut(String message) {
        for (String key : shortcuts.keySet()) {
            if (message.length() > key.length() && message.toLowerCase().startsWith(key)) {
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
                try {
                    registerCommand(key, new CommandAlias(config.getString("BindCommands." + key)), this,
                            new ArrayList<String>(), false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
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
            Field commands = this.getDescription().getClass().getDeclaredField("commands");
            commands.setAccessible(true);
            commands.set(this.getDescription(), commandsMap);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        ChannelListeners listeners = new ChannelListeners(this);
        Bukkit.getPluginManager().registerEvents(listeners, this);
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        Bukkit.getMessenger().registerIncomingPluginChannel(this, "BungeeCord", listeners);
        Bukkit.getMessenger().registerIncomingPluginChannel(this, "BungeeCord", chatManager);
        loadConfig();
        final Main main = this;
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
                try {
                    if (key.equals("Channel")) {
                        registerCommand(commandName, new ChannelCommand(main, commandName), this, aliases, false);
                    } else if (key.equals("Message")) {
                        registerCommand(commandName, new Message(chatManager, commandName), this, aliases, false);
                    } else if (key.equals("Reply")) {
                        registerCommand(commandName, new Reply(chatManager), this, aliases, false);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
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
        try {
            ((SimpleCommandMap) Bukkit.getServer().getClass().getDeclaredMethod("getCommandMap").invoke(Bukkit.getServer()))
                    .registerAll(this.getDescription().getName(), PluginCommandYamlParser.parse(this));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void registerCommand(final String name, final CommandExecutor exc, final JavaPlugin plugin, List<String> aliases,
            boolean isAlias) throws Exception {
        String desc = null;
        if (!isAlias) {
            for (String alias : aliases) {
                registerCommand(alias, exc, plugin, new ArrayList<String>(), true);
            }
        }
        try {
            Field field = exc.getClass().getDeclaredField("description");
            desc = ChatColor.translateAlternateColorCodes('&', (String) field.get(exc));
        } catch (Exception ex) {
        }
        HashMap<String, Object> newMap = new HashMap<String, Object>();
        if (desc != null) {
            newMap.put("description", desc);
        }
        commandsMap.put(name.toLowerCase(), newMap);
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            public void run() {
                PluginCommand command = plugin.getCommand(name.toLowerCase());
                if (command != null) {
                    command.setExecutor(exc);
                } else {
                    System.out.print(String.format("Error while registering command %s: %s", name, "Can't register command"));
                }
            }
        });
    }

    public void removeFromChannel(ChatChannel channel, Player player) {
        if (players.containsKey(channel)) {
            players.get(channel).remove(player);
            if (players.get(channel).size() == 0)
                players.remove(channel);
        }
    }
}
