package com.rtpqueuez.config;

import com.rtpqueuez.RTPQueueZ;
import com.rtpqueuez.util.ChatUtil;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class QueueConfig {

    private final RTPQueueZ plugin;
    private FileConfiguration messagesConfig;
    private File messagesFile;

    private int teleportDelay;
    private int cooldown;
    private int maxPlayersPerQueue;
    private String menuTitle;
    private int menuSize;
    private final Map<String, Map<String, Object>> menuItems = new HashMap<>();
    private final Map<String, List<String>> messages = new HashMap<>();
    private final Map<String, String> titles = new HashMap<>();

    public QueueConfig(RTPQueueZ plugin) {
        this.plugin = plugin;
    }

    public void loadConfigs() {
        // Load config.yml settings
        FileConfiguration config = plugin.getConfig();
        teleportDelay = config.getInt("teleport-delay", 5);
        cooldown = config.getInt("cooldown", 30);
        maxPlayersPerQueue = config.getInt("max-players-per-queue", 2);

        menuTitle = ChatUtil.color(config.getString("menu.title", "&b&lRTP Queue Menu"));
        menuSize = config.getInt("menu.size", 36);

        // Load menu items
        menuItems.clear();
        if (config.isConfigurationSection("menu.items")) {
            for (String key : config.getConfigurationSection("menu.items").getKeys(false)) {
                Map<String, Object> itemData = new HashMap<>();
                itemData.put("Material", config.getString("menu.items." + key + ".Material"));
                itemData.put("slot", config.getInt("menu.items." + key + ".slot"));
                itemData.put("display_name", config.getString("menu.items." + key + ".display_name"));
                itemData.put("lore", config.getStringList("menu.items." + key + ".lore"));
                itemData.put("actions", config.getStringList("menu.items." + key + ".actions"));
                menuItems.put(key, itemData);
            }
        }

        // Load messages.yml
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        // Load chat messages
        messages.clear();
        for (String key : messagesConfig.getConfigurationSection("").getKeys(false)) {
            if (messagesConfig.isList(key)) {
                messages.put(key, messagesConfig.getStringList(key));
            }
        }
        
        // Load titles
        titles.clear();
        titles.put("teleport", ChatUtil.color(messagesConfig.getString("titles.teleport", "&a&lACCEPTED")));
        titles.put("teleport-subtitle", ChatUtil.color(messagesConfig.getString("titles.subtitles.teleport", "&aTeleporting...")));
    }

    public Optional<Inventory> createQueueMenu(String title, int size, Player player) {
        try {
            Inventory menu = Bukkit.createInventory(null, size, title);

            for (Map.Entry<String, Map<String, Object>> entry : menuItems.entrySet()) {
                Map<String, Object> itemData = entry.getValue();

                Material material = Material.getMaterial((String) itemData.getOrDefault("Material", "STONE"));
                int slot = (int) itemData.getOrDefault("slot", 0);
                String displayName = (String) itemData.getOrDefault("display_name", "&fDefault Name");
                List<String> lore = (List<String>) itemData.getOrDefault("lore", new ArrayList<>());

                ItemStack item = new ItemStack(material != null ? material : Material.STONE);
                ItemMeta meta = item.getItemMeta();

                // Apply Placeholders to Name and Lore
                String finalDisplayName = PlaceholderAPI.setPlaceholders(player, displayName);
                meta.setDisplayName(ChatUtil.color(finalDisplayName));

                List<String> finalLore = new ArrayList<>();
                for (String line : lore) {
                    finalLore.add(ChatUtil.color(PlaceholderAPI.setPlaceholders(player, line)));
                }
                meta.setLore(finalLore);

                item.setItemMeta(meta);

                if (slot >= 0 && slot < size) {
                    menu.setItem(slot, item);
                } else {
                    plugin.getLogger().warning("Invalid slot (" + slot + ") for item: " + entry.getKey());
                }
            }

            return Optional.of(menu);

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to create RTP Queue Menu: " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * Attempts to find the config key for a given ItemStack, primarily used for menu listeners.
     * This method is a simplified example and assumes unique names/lore are enough for identification.
     */
    public Optional<String> getItemKeyByItemStack(ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return Optional.empty();

        // Strip color codes and placeholders for a loose comparison
        String clickedName = ChatUtil.stripColor(meta.getDisplayName()).trim();

        for (String key : menuItems.keySet()) {
            String configName = (String) menuItems.get(key).getOrDefault("display_name", "");
            
            // PlaceholderAPI usage here would be complex, so we check the raw, unparsed name
            // In a full implementation, you'd compare item NBT/tags.
            String rawConfigName = ChatUtil.stripColor(configName).trim();
            if (rawConfigName.equalsIgnoreCase(clickedName)) {
                return Optional.of(key);
            }
        }
        return Optional.empty();
    }
    
    public List<String> getItemActions(String itemKey) {
        return (List<String>) menuItems.getOrDefault(itemKey, new HashMap<>()).get("actions");
    }

    /**
     * Sends messages from messages.yml to a player, replacing placeholders.
     */
    public void sendMessages(Player player, List<String> messages, String worldPlaceholder, String countPlaceholder) {
        if (messages == null) return;
        for (String line : messages) {
            line = line.replace("{world}", worldPlaceholder);
            line = line.replace("{count}", countPlaceholder);
            player.sendMessage(ChatUtil.color(line));
        }
    }

    public void sendMessages(Player player, List<String> messages, String worldPlaceholder) {
        sendMessages(player, messages, worldPlaceholder, ""); // Overload for simpler calls
    }

    public void sendMessages(Player player, List<String> messages) {
        sendMessages(player, messages, "", ""); // Overload for no world/count placeholders
    }

    /**
     * Sends a broadcast message from messages.yml, replacing placeholders.
     */
    public void broadcastMessages(Player player, List<String> messages, String worldName) {
        if (messages == null) return;
        for (String line : messages) {
            line = line.replace("{world}", worldName);
            line = line.replace("{player}", player.getName());
            Bukkit.broadcastMessage(ChatUtil.color(line));
        }
    }

    /**
     * Sends a title and subtitle from messages.yml
     */
    public void sendTitle(Player player, String key) {
        String title = titles.get(key);
        String subtitle = titles.get(key + "-subtitle");
        if (title != null && subtitle != null) {
            player.sendTitle(title, subtitle, 10, 40, 10);
        }
    }


    // Getters
    public int getTeleportDelay() { return teleportDelay; }
    public int getCooldown() { return cooldown; }
    public int getMaxPlayersPerQueue() { return maxPlayersPerQueue; }
    public String getMenuTitle() { return menuTitle; }
    public int getMenuSize() { return menuSize; }
    public Map<String, List<String>> getMessages() { return messages; }
}
