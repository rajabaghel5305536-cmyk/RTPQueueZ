package com.rtpqueuez;

import com.rtpqueuez.commands.RTPQueueCommand;
import com.rtpqueuez.config.QueueConfig;
import com.rtpqueuez.listeners.MenuListener;
import com.rtpqueuez.placeholders.RTPQueuePlaceholder;
import com.rtpqueuez.queue.QueueManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class RTPQueueZ extends JavaPlugin {

    private QueueManager queueManager;
    private QueueConfig queueConfig;
    private static RTPQueueZ instance;

    @Override
    public void onEnable() {
        instance = this;

        // 1. Load Configurations
        this.saveDefaultConfig();
        this.queueConfig = new QueueConfig(this);
        this.queueConfig.loadConfigs();

        // 2. Initialize Core Manager
        this.queueManager = new QueueManager(this, queueConfig);

        // 3. Register Command
        this.getCommand("rtpqueue").setExecutor(new RTPQueueCommand(this, queueManager));

        // 4. Register Event Listener (for the menu)
        Bukkit.getPluginManager().registerEvents(new MenuListener(this, queueConfig), this);

        // 5. Register PlaceholderAPI Hook
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new RTPQueuePlaceholder(queueManager).register();
            getLogger().info("PlaceholderAPI hook registered successfully.");
        } else {
            getLogger().warning("PlaceholderAPI not found. Placeholders will not work.");
        }

        getLogger().info("RTPQueueZ v" + getDescription().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        // Stop any running tasks and clear queues
        Bukkit.getScheduler().cancelTasks(this);
        getLogger().info("RTPQueueZ v" + getDescription().getVersion() + " disabled.");
    }

    public static RTPQueueZ getInstance() {
        return instance;
    }

    public QueueManager getQueueManager() {
        return queueManager;
    }

    public QueueConfig getQueueConfig() {
        return queueConfig;
    }
}
