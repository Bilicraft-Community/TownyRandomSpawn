package com.mcsunnyside.randomtownyspawn;

import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

public class RandomTownySpawnEvent extends Event implements Listener {
    private final static HandlerList handlerList = new HandlerList();
    private final Town town;
    private boolean cancelled = false;

    public RandomTownySpawnEvent(Town town) {
        this.town = town;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }

    public Town getTown() {
        return town;
    }


    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
