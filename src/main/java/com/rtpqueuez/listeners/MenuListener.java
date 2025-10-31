package com.rtpqueuez.listeners;

import com.rtpqueuez.RTPQueueZ;
import com.rtpqueuez.config.QueueConfig;
import com.rtpqueuez.util.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Optional;

public class MenuListener implements Listener {

    private final RTPQueueZ plugin;
    private final QueueConfig config;

    public MenuListener(RTPQueueZ plugin, QueueConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Inventory clickedInventory = event.getClickedInventory();
        ItemStack currentItem = event.getCurrentItem();

        // Check if the clicked inventory is one of our queue menus
        if (clickedInventory == null || !event.getView().getTitle().equals(config.getMenuTitle())) {
            return;
        }

        event.setCancelled(true);

        if (currentItem == null || currentItem.getType() == Material.AIR) {
            return;
        }

        // Get the item key from the configuration (e.g., 'overworld', 'nether')
        Optional<String> itemKey = config.getItemKeyByItemStack(currentItem);

        if (itemKey.isPresent()) {
            List<String> actions = config.getItemActions(itemKey.get());

            if (actions != null && !actions.isEmpty()) {
                player.closeInventory();
                
                // Execute all commands configured for this menu item
                for (String action : actions) {
                    if (action.startsWith("rtpqueue ")) {
                        // Delay execution slightly to ensure inventory is fully closed
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                String[] parts = action.split(" ");
                                // Dispatch the command to the main command handler
                                Bukkit.dispatchCommand(player, parts[0] + " " + parts[1]);
                            }
                        }.runTaskLater(plugin, 1L);
                    } else {
                        // Execute other configured console/player commands here if needed
                        player.sendMessage(ChatUtil.color("&cUnknown menu action: " + action));
                    }
                }
            }
        }
    }
}
