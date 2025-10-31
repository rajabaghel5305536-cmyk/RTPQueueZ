package com.rtpqueuez.queue;

import com.rtpqueuez.RTPQueueZ;
import com.rtpqueuez.config.QueueConfig;
import com.rtpqueuez.util.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class QueueManager {

    private final RTPQueueZ plugin;
    private final QueueConfig config;
    private final Map<String, List<UUID>> worldQueues = new HashMap<>();
    private final Map<UUID, Long> playerCooldowns = new HashMap<>();

    public QueueManager(RTPQueueZ plugin, QueueConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    /**
     * Attempts to join a player to a world queue.
     */
    public void joinQueue(Player player, String worldName) {
        World targetWorld = Bukkit.getWorld(worldName);

        if (targetWorld == null) {
            config.sendMessages(player, config.getMessages().get("invalid-world"), worldName);
            return;
        }

        // 1. Check Cooldown
        long now = System.currentTimeMillis();
        long cooldownEnd = playerCooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (now < cooldownEnd) {
            long remaining = (cooldownEnd - now) / 1000;
            String message = ChatUtil.replacePlaceholders(config.getMessages().get("cooldown-active").get(0), "{cooldown}", String.valueOf(remaining));
            player.sendMessage(ChatUtil.color(message));
            return;
        }

        // 2. Check if already in queue
        if (isPlayerInAnyQueue(player)) {
            config.sendMessages(player, config.getMessages().get("already-in-queue"), worldName);
            return;
        }

        // 3. Add to queue
        worldQueues.computeIfAbsent(worldName.toLowerCase(), k -> new ArrayList<>()).add(player.getUniqueId());
        config.sendMessages(player, config.getMessages().get("queue-joined"), worldName);

        // 4. Handle queue population
        checkQueueReadiness(worldName);
    }

    /**
     * Checks if a world queue is full and starts the teleport process if ready.
     */
    public void checkQueueReadiness(String worldName) {
        String key = worldName.toLowerCase();
        List<UUID> queue = worldQueues.get(key);

        if (queue == null || queue.isEmpty()) return;

        int maxPlayers = config.getMaxPlayersPerQueue();
        if (queue.size() >= maxPlayers) {
            // Queue is full, start the teleport for the required number of players
            List<UUID> playersToTeleport = new ArrayList<>(queue.subList(0, maxPlayers));
            playersToTeleport.forEach(uuid -> queue.remove(uuid)); // Remove them from the queue

            startTeleportProcess(playersToTeleport, worldName);

            // Broadcast the queue join message for the first player
            Player firstPlayer = Bukkit.getPlayer(playersToTeleport.get(0));
            if (firstPlayer != null) {
                config.broadcastMessages(firstPlayer, config.getMessages().get("queue-joined-broadcast"), worldName);
            }
        } else {
            // Not enough players, send status message to the newest player
            Player newestPlayer = Bukkit.getPlayer(queue.get(queue.size() - 1));
            if (newestPlayer != null) {
                config.sendMessages(newestPlayer, config.getMessages().get("not-enough-players"), worldName, String.valueOf(queue.size()));
            }
        }
    }

    /**
     * Starts the countdown and teleport for a group of players.
     */
    private void startTeleportProcess(List<UUID> players, String worldName) {
        int delay = config.getTeleportDelay();
        long cooldown = config.getCooldown() * 1000L;

        // Send initial teleport message and title
        players.stream()
                .map(Bukkit::getPlayer)
                .filter(p -> p != null && p.isOnline())
                .forEach(p -> {
                    config.sendMessages(p, config.getMessages().get("queue-teleport"), worldName);
                    config.sendTitle(p, "teleport");
                    p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                });

        // Countdown logic
        for (int i = delay; i > 0; i--) {
            int time = i;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                players.stream()
                        .map(Bukkit::getPlayer)
                        .filter(p -> p != null && p.isOnline())
                        .forEach(p -> {
                            // Update countdown message
                            String msg = ChatUtil.replacePlaceholders(config.getMessages().get("teleport").get(0), "{time}", String.valueOf(time));
                            p.sendMessage(ChatUtil.color(msg));
                            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f);
                        });
            }, 20L * (delay - i));
        }

        // Final teleport after delay
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            players.stream()
                    .map(Bukkit::getPlayer)
                    .filter(p -> p != null && p.isOnline())
                    .forEach(p -> {
                        performRTP(p, worldName);
                        // Set cooldown
                        playerCooldowns.put(p.getUniqueId(), System.currentTimeMillis() + cooldown);
                    });
            // Recursively check the queue again in case more players joined during the delay
            checkQueueReadiness(worldName);
        }, 20L * delay);
    }

    /**
     * Performs the actual Random Teleport.
     */
    private void performRTP(Player player, String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return;

        // Simple RTP logic: generate a random location
        int x = (int) (Math.random() * 20000 - 10000);
        int z = (int) (Math.random() * 20000 - 10000);
        int y = world.getHighestBlockYAt(x, z) + 2;

        Location rtpLoc = new Location(world, x, y, z);
        player.teleport(rtpLoc);

        player.sendMessage(ChatUtil.color("&aYou have been successfully teleported to &b" + worldName + "&a!"));
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
    }

    /**
     * Removes a player from their current queue.
     */
    public void leaveQueue(Player player) {
        for (List<UUID> queue : worldQueues.values()) {
            if (queue.remove(player.getUniqueId())) {
                config.sendMessages(player, config.getMessages().get("queue-leaved"));
                return;
            }
        }
    }

    /**
     * Gets the number of players in a specific world queue.
     */
    public int getQueueCount(String worldName) {
        List<UUID> queue = worldQueues.get(worldName.toLowerCase());
        return queue != null ? queue.size() : 0;
    }

    /**
     * Checks if a player is in any queue.
     */
    public boolean isPlayerInAnyQueue(Player player) {
        for (String worldName : worldQueues.keySet()) {
            if (worldQueues.get(worldName).contains(player.getUniqueId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the world name the player is currently queued for.
     */
    public String getQueuedWorld(Player player) {
        for (Map.Entry<String, List<UUID>> entry : worldQueues.entrySet()) {
            if (entry.getValue().contains(player.getUniqueId())) {
                return entry.getKey();
            }
        }
        return null;
    }
}
