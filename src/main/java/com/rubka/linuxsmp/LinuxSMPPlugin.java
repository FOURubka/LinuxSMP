package com.rubka.linuxsmp;

import org.bukkit.plugin.java.JavaPlugin;

public class LinuxSMPPlugin extends JavaPlugin {

    private DistroManager distroManager;
    private PlayerDataManager playerDataManager;
    private SkillCheckService skillCheckService;
    private ItemFactory itemFactory;
    private CombatTracker combatTracker;
    private ArenaManager arenaManager;
    private EconomyHook economyHook;
    private FusionRecipeRegistry fusionRecipeRegistry;
    private BossBarManager bossBars;
    private AbilityListener abilityListener;
    private PetManager petManager;
    private BlockTransformService blockTransforms;
    private RewindService rewindService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        distroManager = new DistroManager(this);
        distroManager.load();
        itemFactory = new ItemFactory(this);
        playerDataManager = new PlayerDataManager(this);
        skillCheckService = new SkillCheckService(this);
        combatTracker = new CombatTracker();
        arenaManager = new ArenaManager(this);
        economyHook = new EconomyHook(this);
        economyHook.setup();
        fusionRecipeRegistry = new FusionRecipeRegistry(this);
        fusionRecipeRegistry.register();
        bossBars = new BossBarManager(this);
        bossBars.start();
        blockTransforms = new BlockTransformService(this);
        rewindService = new RewindService(this);
        rewindService.start();

        var pm = getServer().getPluginManager();
        pm.registerEvents(playerDataManager, this);
        pm.registerEvents(combatTracker, this);
        pm.registerEvents(arenaManager, this);
        pm.registerEvents(fusionRecipeRegistry, this);
        abilityListener = new AbilityListener(this);
        pm.registerEvents(abilityListener, this);
        pm.registerEvents(new LootListener(this), this);
        pm.registerEvents(new GuiListener(), this);
        pm.registerEvents(new JoinRerollListener(this), this);

        getCommand("distroroll").setExecutor(new RollCommand(this));
        getCommand("distroswap").setExecutor(new SwapCommand(this));
        getCommand("distros").setExecutor(new RosterCommand(this));
        getCommand("distrotoken").setExecutor(new TokenCommand(this));
        getCommand("distrorepair").setExecutor(new RepairCommand(this));
        getCommand("distrofuse").setExecutor(new FusionCommand(this));
        getCommand("distrotop").setExecutor(new DistroTopCommand(this));
        getCommand("distroduel").setExecutor(new DuelCommand(this));
        getCommand("pacman-syu").setExecutor(new MaintenanceCommand(this));
        getCommand("linuxsmp").setExecutor(new AdminCommand(this));

        LinuxTab tab = new LinuxTab(this);
        for (String cmd : new String[]{"distroroll", "distroswap", "distrotoken", "distrorepair", "distroduel", "linuxsmp"}) {
            getCommand(cmd).setTabCompleter(tab);
        }

        // One shortcut command per distro (e.g. /nixos) that fires its ability.
        DistroAbilityCommand distroCmd = new DistroAbilityCommand(this);
        for (String id : distroManager.all().keySet()) {
            var cmd = getCommand(id);
            if (cmd != null) {
                cmd.setExecutor(distroCmd);
                cmd.setTabCompleter(distroCmd);
            } else {
                getLogger().warning("No command registered in plugin.yml for distro '" + id + "'.");
            }
        }

        new MaintenanceTask(this).runTaskTimer(this, 100L, 100L);
        new AmbientTask(this).runTaskTimer(this, 40L, 10L);
        new PassiveTask(this).runTaskTimer(this, 60L, 100L);
        new SynergyTask(this).runTaskTimer(this, 80L, 20L);
        petManager = new PetManager(this);
        petManager.runTaskTimer(this, 20L, 3L);
        // Periodic autosave every 5 minutes.
        getServer().getScheduler().runTaskTimer(this, () -> playerDataManager.saveAll(), 6000L, 6000L);
    }

    @Override
    public void onDisable() {
        if (petManager != null) {
            petManager.stopAll();
        }
        if (playerDataManager != null) {
            playerDataManager.saveAll();
        }
        if (fusionRecipeRegistry != null) {
            fusionRecipeRegistry.unregister();
        }
    }

    public void reloadAll() {
        reloadConfig();
        distroManager.load();
        fusionRecipeRegistry.register();
    }

    public DistroManager getDistroManager() {
        return distroManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public SkillCheckService getSkillCheckService() {
        return skillCheckService;
    }

    public ItemFactory getItemFactory() {
        return itemFactory;
    }

    public CombatTracker getCombatTracker() {
        return combatTracker;
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public EconomyHook getEconomyHook() {
        return economyHook;
    }

    public BossBarManager getBossBars() {
        return bossBars;
    }

    public AbilityListener getAbilityListener() {
        return abilityListener;
    }

    public BlockTransformService getBlockTransforms() {
        return blockTransforms;
    }

    public RewindService getRewind() {
        return rewindService;
    }
}
