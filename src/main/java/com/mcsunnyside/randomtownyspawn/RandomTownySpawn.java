package com.mcsunnyside.randomtownyspawn;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class RandomTownySpawn extends JavaPlugin implements Listener {
    //private TownBlockType townBlockType;
    private Towny towny;
    private List<String> towns;
    private boolean whitelistMode;
    private RandomTownySpawn instance;
    private int minBlockAmount = 8;
    private int minResidentsAmount = 3;
    private int maxResidentsAmount = -1;
    private final Set<TownBlockType> townBlockTypes = new HashSet<>();
    private boolean autoJoinTown = false;
    private final ThreadLocalRandom random = ThreadLocalRandom.current();

    @Override
    public void onEnable() {
        // Plugin startup logic
        this.instance = this;
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this,this);
        towny = (Towny) Bukkit.getPluginManager().getPlugin("Towny");
        townBlockTypes.clear();
        //townBlockType = TownBlockType.lookup(getConfig().getString("town-type", "embassy"));
        towns = getConfig().getStringList("list.towns");
        whitelistMode = getConfig().getBoolean("list.whitelist-mode");
        minBlockAmount = getConfig().getInt("limit.min-block-amount");
        minResidentsAmount = getConfig().getInt("limit.min-residents-amount");
        maxResidentsAmount = getConfig().getInt("limit.max-residents-amount");
        autoJoinTown = getConfig().getBoolean("auto-join-the-town");
        getConfig().getStringList("limit.type-of-blocks-included").forEach(type -> {
            TownBlockType townBlockType = TownBlockType.lookup(type);
            if (townBlockType == null) {
                getLogger().warning(type + " type invalid!");
                return;
            }
            townBlockTypes.add(townBlockType);
        });
        //getLogger().info("Plugin running under " + (whitelistMode ? "whitelist mode" : "blacklist mode") + " The town in list: " + towns.toString());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onPlayerJoining(PlayerJoinEvent event) {
        new BukkitRunnable() {
            @Override
            public void run() {
                List<Town> okTown = new ArrayList<>();
                if (event.getPlayer().hasPlayedBefore()) {
                    return;
                }
                for (Town town : TownyAPI.getInstance().getDataSource().getTowns()) {
                    //Whitelist check
                    if (!listCheck(town)) {
                        continue;
                    }
                    //The limits check
                    if (!blocksCheck(town)) {
                        continue;
                    }
                    if (!residentsCheck(town)) {
                        continue;
                    }
                    if (!blocksTypeCheck(town)) {
                        continue;
                    }

                   okTown.add(town);
                }
                if(okTown.isEmpty()) {
                    getLogger().info("No any towns matches the limits, player " + event.getPlayer().getName() + " will use default spawn point.");
                    return;
                }
                int randomInt;
                if(okTown.size() == 1){
                    randomInt = 0;
                }else{
                    randomInt = random.nextInt(okTown.size()-1);
                }

                Town town = okTown.get(randomInt);
                try {
                    Location location = town.getSpawn();
                    Bukkit.getScheduler().runTask(instance, () -> {
                        RandomTownySpawnEvent spawnEvent = new RandomTownySpawnEvent(town);
                        if (spawnEvent.isCancelled()) {
                            getLogger().info("Player " + event.getPlayer().getName() + " random spawn event was cancelled.");
                            return;
                        }
                        event.getPlayer().teleport(location);
                        getLogger().info("Spawning player " + event.getPlayer().getName() + " to town " + town.getName());
                        if (autoJoinTown) {
                            try {
                                Resident resident = TownyAPI.getInstance().getDataSource().getResident(event.getPlayer().getName());
                                town.addResident(resident);
                                towny.deleteCache(resident.getName());
                                TownyUniverse townyUniverse = TownyUniverse.getInstance();
                                townyUniverse.getDataSource().saveResident(resident);
                                townyUniverse.getDataSource().saveTown(town);
                                getLogger().info("Successfully add player " + event.getPlayer().getName() + " to town " + town.getName());
                            } catch (AlreadyRegisteredException | NotRegisteredException e) {
                                e.printStackTrace();
                                getLogger().info("Failed to add player " + event.getPlayer().getName() + " to town " + town.getName());
                            }
                        }
                    });
                } catch (TownyException ignored) {
                }
            }
        }.runTaskAsynchronously(this);
    }

    private boolean blocksTypeCheck(Town town) {
        Set<TownBlockType> types = new HashSet<>();
        for (TownBlock block : town.getTownBlocks()) {
            types.add(block.getType());
        }
        return types.containsAll(this.townBlockTypes);
    }

    private boolean blocksCheck(Town town) {
        return town.getTotalBlocks() >= this.minBlockAmount;
    }

    private boolean residentsCheck(Town town) {
        if (this.minResidentsAmount > 0) {
            if (town.getNumResidents() < this.minResidentsAmount) {
                return false;
            }
        }

        if (this.maxResidentsAmount > 0) {
            //noinspection RedundantIfStatement
            if (town.getNumResidents() > this.maxResidentsAmount) {
                return false;
            }
        }

        return true;
    }

    /**
     * Check the town passed the list check
     *
     * @param town Town
     * @return passed
     */
    private boolean listCheck(Town town) {
        boolean inList = towns.contains(town.getName());
        if (whitelistMode) {
            return inList;
        } else {
            return !inList;
        }
    }
}
