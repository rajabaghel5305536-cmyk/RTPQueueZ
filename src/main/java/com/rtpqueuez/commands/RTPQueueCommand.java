package com.rtpqueuez.commands;

import com.rtpqueuez.RTPQueueZ;
import com.rtpqueuez.config.QueueConfig;
import com.rtpqueuez.queue.QueueManager;
import com.rtpqueuez.util.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.Optional;

public class RTPQueueCommand implements CommandExecutor {

    private final RTPQueueZ plugin;
    private final QueueManager queueManager;
    private final QueueConfig config;

    public RTPQueueCommand(RTPQueueZ plugin, QueueManager queueManager) {
        this.plugin = plugin;
        this.queueManager = queueManager;
        this.config = plugin.getQueueConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtil.color("&cOnly players can use this command."));
            return true;
        }

        if (!player.hasPermission("rtpqueue.use")) {
            config.sendMessages(player, config.getMessages().get("no-permission"));
            return true;
        }

        if (args.length == 0) {
            // /rtpqueue - Open menu
            openQueueMenu(player);
            return true;
        }

        if (args.length == 1) {
            // /rtpqueue <world> - Queue for world
            String worldName = args[0].toLowerCase();
            queueManager.joinQueue(player, worldName);
            return true;
        }

        // Help or default case
        player.sendMessage(ChatUtil.color("&a&lRTPQueueZ &fUsage:"));
        player.sendMessage(ChatUtil.color("&b/rtpqueue &f- Open the queue menu."));
        player.sendMessage(ChatUtil.color("&b/rtpqueue <world> &f- Join the queue for a world."));
        return true;
    }

    /**
     * Creates and opens the custom inventory menu based on config.yml.
     * @param player The player to open the menu for.
     */
    private void openQueueMenu(Player player) {
        String title = config.getMenuTitle();
        int size = config.getMenuSize();

        // Use the Inventory utility class to replace placeholders on the fly
        Optional<Inventory> menu = config.createQueueMenu(title, size, player);

        menu.ifPresentOrElse(player::openInventory, () -> {
            player.sendMessage(ChatUtil.color("&cError: Could not load queue menu from config!"));
        });
    }
}
