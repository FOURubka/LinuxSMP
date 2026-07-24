package com.rubka.linuxsmp;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PlayerDistroData {

    public final UUID uuid;
    public final Set<String> unlocked = new LinkedHashSet<>();
    public final Set<String> permaBricked = new LinkedHashSet<>();
    public final Map<String, Long> tempBrickedUntil = new HashMap<>();
    public String active;
    public boolean mixedBuild;
    public long lastMaintenance;
    public long swapCooldownUntil;
    public long abilityCooldownUntil;
    // NixOS rollback (in-memory only, not persisted)
    public transient long rollbackCooldownUntil;
    // Join reroll bookkeeping (persisted)
    public boolean joinRerolled;
    public long lastJoinReroll;
    public int skillLevel;
    public long packages;
    public long repoTokens;
    public long dotfiles;

    public PlayerDistroData(UUID uuid) {
        this.uuid = uuid;
    }

    public boolean isTempBricked(String distroId) {
        Long until = tempBrickedUntil.get(distroId);
        if (until == null) {
            return false;
        }
        if (until <= System.currentTimeMillis()) {
            tempBrickedUntil.remove(distroId);
            return false;
        }
        return true;
    }

    public long tempBrickSecondsLeft(String distroId) {
        Long until = tempBrickedUntil.get(distroId);
        return until == null ? 0 : Math.max(0, (until - System.currentTimeMillis()) / 1000);
    }
}
