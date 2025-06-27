package org.iridiummc.huntPoints;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;

class HuntPointCommand implements CommandExecutor {

    private final HuntPoints plugin;

    public HuntPointCommand(HuntPoints plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be run by a player.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("huntpoint.admin")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("addhuntpoint")) {
            if (args.length == 3) {
                try {
                    double x = Double.parseDouble(args[0]);
                    double y = Double.parseDouble(args[1]);
                    double z = Double.parseDouble(args[2]);

                    Location huntLocation = new Location(player.getWorld(), x, y, z);

                    if (plugin.addHuntPoint(huntLocation)) {
                        player.sendMessage(ChatColor.GREEN + "Hunt point added at X:" + x + " Y:" + y + " Z:" + z + " in world " + player.getWorld().getName() + ".");
                        player.sendMessage(ChatColor.GOLD + "Total hunt points now: " + plugin.getTotalHuntPoints());
                    } else {
                        player.sendMessage(ChatColor.YELLOW + "Hunt point already exists at X:" + x + " Y:" + y + " Z:" + z + ".");
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Invalid coordinates. Please use numbers for X, Y, and Z.");
                    player.sendMessage(ChatColor.RED + "Usage: /addhuntpoint <x> <y> <z>");
                }
            } else {
                player.sendMessage(ChatColor.RED + "Usage: /addhuntpoint <x> <y> <z>");
            }
            return true;
        }
        else if (command.getName().equalsIgnoreCase("removehuntpoint")) {
            if (args.length == 3) {
                try {
                    double x = Double.parseDouble(args[0]);
                    double y = Double.parseDouble(args[1]);
                    double z = Double.parseDouble(args[2]);

                    Location huntLocation = new Location(player.getWorld(), x, y, z);

                    if (plugin.removeHuntPoint(huntLocation)) {
                        player.sendMessage(ChatColor.GREEN + "Hunt point removed at X:" + x + " Y:" + y + " Z:" + z + " in world " + player.getWorld().getName() + ".");
                        player.sendMessage(ChatColor.GOLD + "Total hunt points now: " + plugin.getTotalHuntPoints());
                    } else {
                        player.sendMessage(ChatColor.YELLOW + "No hunt point found at X:" + x + " Y:" + y + " Z:" + z + ".");
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Invalid coordinates. Please use numbers for X, Y, and Z.");
                    player.sendMessage(ChatColor.RED + "Usage: /removehuntpoint <x> <y> <z>");
                }
            } else {
                player.sendMessage(ChatColor.RED + "Usage: /removehuntpoint <x> <y> <z>");
            }
            return true;
        } else if (command.getName().equalsIgnoreCase("huntmode")) {
            if (!player.hasPermission("huntpoint.admin")) {
                player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                return true;
            }

            if (plugin.toggleHuntMode(player)) {
                player.sendMessage(ChatColor.GREEN + "You have entered hunt mode. Hunt points are now visible.");
            } else {
                player.sendMessage(ChatColor.YELLOW + "You have exited hunt mode. Hunt points are no longer visible.");
            }
            return true;
        }

        return false;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (plugin.isInHuntMode(player)) {
            plugin.toggleHuntMode(player);
        }
    }
}
