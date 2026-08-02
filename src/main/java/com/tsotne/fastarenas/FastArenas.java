package com.tsotne.fastarenas;

import com.sk89q.worldedit.world.registry.BundledBlockData.BlockEntry;
import com.tsotne.fastarenas.Config.ConfigManager;
import com.tsotne.fastarenas.Event.ArenaCreate;
import com.tsotne.fastarenas.autoreset.AutoResetManager;
import com.tsotne.fastarenas.commands.Command;
import com.tsotne.fastarenas.utils.LoadSchem;
import it.unimi.dsi.fastutil.Pair;
import java.io.File;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

public final class FastArenas extends JavaPlugin {
    private ConfigManager configManager;
    private AutoResetManager autoResetManager;
    Pair<String, List<BlockEntry>> Arenas;

    public void onEnable() {
        this.getCommand("fastarena").setExecutor(new Command(this));
        this.getServer().getPluginManager().registerEvents(new ArenaCreate(this), this);
        this.configManager = new ConfigManager(this);
        this.saveDefaultConfig();
        this.createSchematicFolder();
        new Metrics(this, 28906);
        this.autoResetManager = new AutoResetManager();
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new FastPlaceHolderAPI(this).register();
        }

        ConfigurationSection sec = this.getConfig().getConfigurationSection("arena");
        if (sec != null) {
            for (String id : sec.getKeys(false)) {
                if (id != null && !id.trim().isEmpty()) {
                    ConfigurationSection arena = sec.getConfigurationSection(id);
                    if (arena == null) {
                        this.getLogger().warning("⚠ Arena section '" + id + "' is missing or not a section.");
                    } else if (arena.contains("pos1") && arena.contains("pos2") && arena.contains("world")) {
                        boolean enabled = arena.getBoolean("enabled", false);
                        long offset = arena.getLong("offset", 0L);
                        long autoreset = arena.getLong("autoreset", 0L);
                        String message = arena.getString("message", "&aArena %arena% has been reseted!");
                        if (enabled && autoreset > 0L) {
                            this.autoResetManager.registerArena(id, autoreset * 20L, offset * 20L, message, this);
                            this.getLogger().info("✔ Loaded autoreset arena: " + id);
                        } else {
                            this.getLogger().info("ℹ Loaded arena '" + id + "' (autoreset disabled).");
                        }
                    } else {
                        this.getLogger().warning("⚠ Arena '" + id + "' is missing required fields (pos1, pos2, world). Skipping.");
                    }
                } else {
                    this.getLogger().warning("⚠ Found arena with invalid ID (empty or null). Skipping.");
                }
            }
        }

        this.reloadEverything();
    }

    public ConfigManager getConfigManager() {
        return this.configManager;
    }

    public AutoResetManager getautoResetManager() {
        return this.autoResetManager;
    }

    public void reloadEverything() {
        this.configManager.reload();
        LoadSchem.clearCache();
    }

    private void createSchematicFolder() {
        File schematicsDir = new File(this.getDataFolder(), "arena");
        if (!schematicsDir.exists()) {
            boolean success = schematicsDir.mkdirs();
            if (!success) {
                this.getLogger().warning("Failed to create arena folder.");
            }
        }
    }
}