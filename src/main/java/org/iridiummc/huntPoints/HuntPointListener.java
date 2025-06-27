package org.iridiummc.huntPoints;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.block.Block;
import org.bukkit.event.player.PlayerQuitEvent;

class HuntPointListener implements Listener {

    private final HuntPoints plugin;

    public HuntPointListener(HuntPoints plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block clickedBlock = event.getClickedBlock();
            if (clickedBlock != null) {
                Location blockLocation = clickedBlock.getLocation();

                if (plugin.isHuntPoint(blockLocation)) {
                    Player player = event.getPlayer();

                    if (plugin.findHuntPoint(player, blockLocation)) {
                        player.sendMessage(ChatColor.GREEN + "Congratulations! You found a hunt point!");
                        player.sendMessage(ChatColor.GOLD + "Remaining hunt points: " + plugin.getRemainingHuntPoints(player));
                    }
                }
            }
        }
        else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            Player player = event.getPlayer();
            Location blockLocation = event.getClickedBlock().getLocation();
            if (plugin.isInHuntMode(player)) {
                event.setCancelled(true);
                if (plugin.isHuntPoint(blockLocation)) {
                    if (plugin.removeHuntPoint(blockLocation)) {
                        player.sendMessage(ChatColor.GREEN + "Hunt point removed at X:" + blockLocation.getBlockX() + " Y:" + blockLocation.getBlockY() + " Z:" + blockLocation.getBlockZ() + ".");
                        player.sendMessage(ChatColor.GOLD + "Total hunt points now: " + plugin.getTotalHuntPoints());
                    } else {
                        player.sendMessage(ChatColor.RED + "Error removing hunt point.");
                    }
                } else {
                    if (plugin.addHuntPoint(blockLocation)) {
                        player.sendMessage(ChatColor.GREEN + "Hunt point added at X:" + blockLocation.getBlockX() + " Y:" + blockLocation.getBlockY() + " Z:" + blockLocation.getBlockZ() + ".");
                        player.sendMessage(ChatColor.GOLD + "Total hunt points now: " + plugin.getTotalHuntPoints());
                    } else {
                        player.sendMessage(ChatColor.RED + "Error adding hunt point.");
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (plugin.isInHuntMode(player)) {
            plugin.toggleHuntMode(player);
        }
    }
}
