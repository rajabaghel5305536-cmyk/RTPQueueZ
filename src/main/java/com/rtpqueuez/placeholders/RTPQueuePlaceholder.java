package com.rtpqueuez.placeholders;

import com.rtpqueuez.queue.QueueManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class RTPQueuePlaceholder extends PlaceholderExpansion {

    private final QueueManager queueManager;

    public RTPQueuePlaceholder(QueueManager queueManager) {
        this.queueManager = queueManager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "rtpqueue";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Gemini";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true; // This placeholder will be registered and remain active
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) {
            return "";
        }

        // %rtpqueue_status_player% - Show player status Queued/Not in Queue
        if (identifier.equalsIgnoreCase("status_player")) {
            String world = queueManager.getQueuedWorld(player);
            if (world != null) {
                return "Queued for " + world;
            }
            return "Not in Queue";
        }

        // %rtpqueue_count_<world>% - how many players are in queue
        if (identifier.startsWith("count_")) {
            String worldName = identifier.substring(6).toLowerCase();
            return String.valueOf(queueManager.getQueueCount(worldName));
        }

        return null;
    }
}
