package com.rubka.linuxsmp;

import java.util.Map;

/** Human-readable name + description for each ability id (for menus/lore). */
public final class Abilities {

    public record Info(String name, String desc) {
    }

    private static final Map<String, Info> INFO = Map.ofEntries(
            Map.entry("community_support", new Info("Community Support", "LTS wave: heal & shield your whole squad")),
            Map.entry("fresh_start", new Info("Fresh Start", "Clean reinstall: cleanse, full food, swap reset")),
            Map.entry("auto_tiling", new Info("Auto-Tiling", "Dash, tile your inventory, snap enemies in place")),
            Map.entry("stable_fortress", new Info("Stable Fortress", "Raise a dome, reflect & shrug off hits")),
            Map.entry("bleeding_edge", new Info("Bleeding Edge", "Blade aura: orbiting swords shred anyone close")),
            Map.entry("featherweight", new Info("Featherweight", "Shrink to a few MB: tiny, fast, floaty")),
            Map.entry("aur_access", new Info("AUR Access", "Air-drop: a crate of real packages from the AUR")),
            Map.entry("pentest", new Info("Stealth / Pentest", "Steal buffs, DDoS abilities, wallhack radar")),
            Map.entry("arch_btw", new Info("I use Arch, btw", "Shockwave burst if maintained, else backfires")),
            Map.entry("compile_everything", new Info("Compile Everything", "Channel 3s, then a huge detonation")),
            Map.entry("from_scratch", new Info("From Scratch", "Full rebuild: god-buffs + massive blast")),
            Map.entry("rollback", new Info("Rollback", "Rewind yourself 10 seconds back in time")),
            Map.entry("anonymity", new Info("Ghost Protocol", "True vanish + decoy; first strike hits x2")),
            Map.entry("old_school", new Info("Old School", "Aimed lightning that chains, may backfire"))
    );

    private Abilities() {
    }

    public static Info of(String id) {
        return INFO.getOrDefault(id, new Info(id, ""));
    }
}
