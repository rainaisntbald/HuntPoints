package org.iridiummc.huntPoints;

import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.concurrent.ConcurrentHashMap;

public class HuntPoints extends JavaPlugin {

    private HashMap<Location, ArrayList<UUID>> huntPointsCache;
    private File huntPointsFile;
    private FileConfiguration huntPointsConfig;

    private Set<Player> playersInHuntMode;

    private BukkitTask particleDisplayTask;
    private ArrayList<Entity> displayEntities = new ArrayList<>();

    @Override
    public void onEnable() {
        this.huntPointsCache = new HashMap<>();
        this.huntPointsFile = new File(getDataFolder(), "huntpoints.yml");
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        loadHuntPoints();
        showBlockEntities();

        this.playersInHuntMode = ConcurrentHashMap.newKeySet();

        getCommand("addhuntpoint").setExecutor(new HuntPointCommand(this));
        getCommand("removehuntpoint").setExecutor(new HuntPointCommand(this));
        getCommand("huntmode").setExecutor(new HuntPointCommand(this));

        getServer().getPluginManager().registerEvents(new HuntPointListener(this), this);

        getLogger().info("HuntPointPlugin has been enabled!");
        getLogger().info("Currently, there are " + huntPointsCache.size() + " hunt points loaded.");
    }

    @Override
    public void onDisable() {
        if (particleDisplayTask != null && !particleDisplayTask.isCancelled()) {
            particleDisplayTask.cancel();
        }
        saveHuntPoints();
        getLogger().info("HuntPointPlugin has been disabled!");
    }

    private void showBlockEntities() {
        particleDisplayTask = getServer().getScheduler().runTaskTimer(this, () -> {
                if (playersInHuntMode.isEmpty() || huntPointsCache.isEmpty()) {
                    return;
                }

                for (Location loc : huntPointsCache.keySet()) {
                    if (loc.getWorld() != null &&
                            displayEntities.stream().noneMatch(e -> loc.getBlock().equals(e.getLocation().getBlock()))) {
                        BlockDisplay block = loc.getWorld().spawn(loc, BlockDisplay.class);
                        block.setBlock(loc.getBlock().getBlockData());

                        block.setInvulnerable(true);
                        block.setGlowing(true);
                        block.setVisibleByDefault(false);
                        for(Player player : Bukkit.getOnlinePlayers()) {
                            if (playersInHuntMode.contains(player)) {
                                player.showEntity(this, block);
                            } else{
                                player.hideEntity(this, block);
                            }
                        }
                        displayEntities.add(block);
                    }
                }
        }, 0L, 10L);
    }

    private void loadHuntPoints() {
        huntPointsConfig = YamlConfiguration.loadConfiguration(huntPointsFile);
        List<Map<?, ?>> pointsList = huntPointsConfig.getMapList("points");
        for (Map<?, ?> map : pointsList) {
            String locString = map.get("location").toString();
            String[] parts = locString.split(",");
            if (parts.length == 4) {
                String worldName = parts[0];
                try {
                    int x = Integer.parseInt(parts[1]);
                    int y = Integer.parseInt(parts[2]);
                    int z = Integer.parseInt(parts[3]);
                    World world = getServer().getWorld(worldName);
                    if (world != null) {
                        Location loc = new Location(world, x, y, z).getBlock().getLocation();
                        List<?> foundList = (List<?>) map.get("found");
                        ArrayList<UUID> foundUUIDs = new ArrayList<>();
                        for (Object uuidStr : foundList) {
                            try {
                                foundUUIDs.add(UUID.fromString(uuidStr.toString()));
                            } catch (IllegalArgumentException e) {
                                getLogger().warning("Invalid UUID in hunt point: " + uuidStr);
                            }
                        }
                        huntPointsCache.put(loc, foundUUIDs);
                    } else {
                        getLogger().warning("World " + worldName + " not found for hunt point: " + locString);
                    }
                } catch (NumberFormatException e) {
                    getLogger().warning("Error parsing coordinates for hunt point: " + locString + " - " + e.getMessage());
                }
            } else {
                getLogger().warning("Invalid hunt point format in file: " + locString);
            }
        }
    }

    private void saveHuntPoints() {
        List<Map<String, Object>> pointsList = new ArrayList<>();
        for (Map.Entry<Location, ArrayList<UUID>> entry : huntPointsCache.entrySet()) {
            Map<String, Object> data = new HashMap<>();
            Location loc = entry.getKey();
            String locString = loc.getWorld().getName() + "," +
                               loc.getBlockX() + "," +
                               loc.getBlockY() + "," +
                               loc.getBlockZ();
            data.put("location", locString);
            List<String> uuidStrings = new ArrayList<>();
            for (UUID uuid : entry.getValue()) {
                uuidStrings.add(uuid.toString());
            }
            data.put("found", uuidStrings);
            pointsList.add(data);
        }
        huntPointsConfig.set("points", pointsList);
        try {
            huntPointsConfig.save(huntPointsFile);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Could not save hunt points to " + huntPointsFile, e);
        }
    }


    public boolean addHuntPoint(Location location) {
        if(huntPointsCache.containsKey(location)) {
            return false;
        }
        Location blockLocation = location.getBlock().getLocation();
        huntPointsCache.put(blockLocation, new ArrayList<>());
        saveHuntPoints();
        return true;
    }

    public boolean removeHuntPoint(Location location) {
        if(!huntPointsCache.containsKey(location)) {
            return false;
        }
        Location blockLocation = location.getBlock().getLocation();
        huntPointsCache.remove(blockLocation);
        saveHuntPoints();
        Iterator<Entity> iterator = displayEntities.iterator();
        while (iterator.hasNext()) {
            Entity e = iterator.next();
            if (e.getLocation().equals(blockLocation)) {
                e.remove();
                iterator.remove();
            }
        }
        return true;
    }

    public boolean isHuntPoint(Location location) {
        return huntPointsCache.containsKey(location.getBlock().getLocation());
    }

    public int getRemainingHuntPoints(Player player) {
        AtomicInteger count = new AtomicInteger();
        huntPointsCache.forEach((location, uuids) -> {
            if(!uuids.contains(player.getUniqueId())) {
                count.getAndIncrement();
            }
        });
        return count.get();
    }

    public boolean findHuntPoint(Player player, Location location) {
        if(!huntPointsCache.containsKey(location.getBlock().getLocation().toBlockLocation()) ||
        huntPointsCache.get(location.getBlock().getLocation()).contains(player.getUniqueId())) {
            return false;
        }
        huntPointsCache.get(location.getBlock().getLocation()).add(player.getUniqueId());
        return true;
    }

    public int getTotalHuntPoints() {
        return huntPointsCache.size();
    }

    public boolean toggleHuntMode(Player player) {
        if (playersInHuntMode.contains(player)) {
            playersInHuntMode.remove(player);
            for(Entity e : displayEntities) {
                player.hideEntity(this, e);
            }
            return false;
        } else {
            playersInHuntMode.add(player);
            for(Entity e : displayEntities) {
                player.showEntity(this, e);
            }
            return true;
        }
    }

    public boolean isInHuntMode(Player player) {
        return playersInHuntMode.contains(player);
    }
}