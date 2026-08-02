package com.tsotne.fastarenas.Config;

import com.tsotne.fastarenas.FastArenas;
import com.tsotne.fastarenas.utils.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {
    private final FastArenas plugin;
    private FileConfiguration config;

    private Set<String> blacklisted = Set.of();
    private boolean blacklistEmpty = true;

    private final Map<String, String> arenaMessageTemplates = new HashMap<>();
    private final Map<String, Location> spawnCache = new HashMap<>();

    private String consoleMessage;
    private String wandName;
    private Material wandMaterial;
    private List<String> wandLore = List.of();
    private String permissionMessage;
    private String notSelectedMessage;
    private String arenaRemovedTemplate;
    private String arenaLoadingError;
    private String positionInvalid;
    private String worldNotSpecified;
    private String worldNotLoaded;
    private String arenaNamedAlreadyTemplate;
    private String arenaSaveTemplate;
    private String arenaFailed;
    private String positionNotSet;
    private String playerSpawnMessage;
    private String spawnSaveTemplate;
    private String spawnNotFoundTemplate;
    private String arenaRemoveFailedTemplate;
    private String arenaNotFound;
    private String arenaFolderMissing;
    private String position1Template;
    private String position2Template;
    private String reloadMessage;
    private String defaultSpawnName;
    private String messageType;

    private int resetBatchSize;
    private long resetBatchDelay;

    public ConfigManager(FastArenas plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        this.reloadCaches();
    }

    public void reload() {
        this.plugin.reloadConfig();
        this.config = this.plugin.getConfig();
        this.reloadCaches();
    }

    private void reloadCaches() {
        this.loadBlacklist();
        this.loadStaticMessages();
        this.loadBatchSettings();
        this.loadSpawns();
        this.loadArenaMessages();
    }

    private void loadStaticMessages() {
        this.consoleMessage = color(this.config.getString("console-message", "&cOnly players can use this command."));
        this.wandName = color(this.config.getString("wand-name", "&6ᴡᴀɴᴅ"));
        this.wandMaterial = parseMaterial(this.config.getString("wand-material", "STICK"), Material.STICK);
        this.wandLore = colorList(this.config.getStringList("wand-lore"));
        this.permissionMessage = color(this.config.getString("permission", "You Don't have Permission To use this command"));
        this.notSelectedMessage = color(this.config.getString("not-selected", "&cYou must select both positions."));
        this.arenaRemovedTemplate = color(this.config.getString("arena-removed", "&aArena %arena% removed."));
        this.arenaLoadingError = color(this.config.getString("arena-loading-error", "§cError loading Arena"));
        this.positionInvalid = color(this.config.getString("position-invalid", "§cInvalid pos1/pos2 in config for arena"));
        this.worldNotSpecified = color(this.config.getString("world-not-specified", "§cWorld not specified for arena"));
        this.worldNotLoaded = color(this.config.getString("world-not-loaded", "§cWorld not loaded"));
        this.arenaNamedAlreadyTemplate = color(this.config.getString("arena-named-already", "§cThere is already an arena named like that %arena%"));
        this.arenaSaveTemplate = color(this.config.getString("arena-save", "Arena saved as %arena%.schem"));
        this.arenaFailed = color(this.config.getString("arena-failed", "§cFailed to save Arena"));
        this.positionNotSet = color(this.config.getString("position-not-set", "§cYou need to select both positions!"));
        this.playerSpawnMessage = color(this.config.getString("player-spawn-message", "§cYou have been Teleported to spawn!"));
        this.spawnSaveTemplate = color(this.config.getString("spawn-save", "§aSuccessfully saved spawn %spawn%!"));
        this.spawnNotFoundTemplate = color(this.config.getString("spawn-not-found", "§cSpawn point %spawn% not found."));
        this.arenaRemoveFailedTemplate = color(this.config.getString("arena-remove-failed", "&cFailed to delete schematic for %arena%."));
        this.arenaNotFound = color(this.config.getString("arena-not-found", "&cArena not found."));
        this.arenaFolderMissing = color(this.config.getString("arena-folder-missing", "&cArena folder does not exist."));
        this.position1Template = color(this.config.getString("position-1-set", "&6Position 1 set ( %pos1_x% %pos1_y% %pos1_z% )"));
        this.position2Template = color(this.config.getString("position-2-set", "&6Position 2 set ( %pos2_x% %pos2_y% %pos2_z% )"));
        this.reloadMessage = color(this.config.getString("reload-complete", "&aFastArenas Reloaded."));
        this.defaultSpawnName = this.config.getString("default-spawn", "default");
        this.messageType = this.config.getString("message-type", "message");
        if (this.messageType != null) {
            this.messageType = this.messageType.toLowerCase(Locale.ROOT);
        } else {
            this.messageType = "message";
        }
    }

    private void loadBatchSettings() {
        this.resetBatchSize = Math.max(1, this.config.getInt("reset-batch-size", 16));
        this.resetBatchDelay = Math.max(0L, this.config.getLong("reset-batch-delay", 1L));
    }

    private void loadSpawns() {
        this.spawnCache.clear();
        ConfigurationSection spawnRoot = this.config.getConfigurationSection("spawn");
        if (spawnRoot == null) {
            return;
        }
        for (String key : spawnRoot.getKeys(false)) {
            ConfigurationSection section = spawnRoot.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            Location location = this.parseSpawnSection(section);
            if (location != null) {
                this.spawnCache.put(key.toLowerCase(Locale.ROOT), location);
            }
        }
    }

    private void loadArenaMessages() {
        this.arenaMessageTemplates.clear();
        ConfigurationSection arenas = this.config.getConfigurationSection("arena");
        if (arenas == null) {
            return;
        }
        String defaultMessage = "&aArena %arena% has been reseted!";
        for (String id : arenas.getKeys(false)) {
            String raw = this.config.getString("arena." + id + ".message", defaultMessage);
            this.arenaMessageTemplates.put(id.toLowerCase(Locale.ROOT), color(raw));
        }
    }

    private static String color(String msg) {
        return Color.translateColors(msg);
    }

    private static List<String> colorList(List<String> list) {
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        List<String> colored = new ArrayList<>(list.size());
        for (String line : list) {
            colored.add(color(line));
        }
        return Collections.unmodifiableList(colored);
    }

    private static Material parseMaterial(String name, Material fallback) {
        if (name == null || name.isEmpty()) {
            return fallback;
        }
        try {
            return Material.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    private static String apply(String template, String placeholder, String value) {
        if (template == null) {
            return "";
        }
        if (value == null) {
            value = "";
        }
        return template.replace(placeholder, value);
    }

    public String getarenamessage(String arenaName) {
        String key = arenaName.toLowerCase(Locale.ROOT);
        String template = this.arenaMessageTemplates.get(key);
        if (template == null) {
            template = color(this.config.getString("arena." + arenaName + ".message", "&aArena %arena% has been reseted!"));
            this.arenaMessageTemplates.put(key, template);
        }
        return apply(template, "%arena%", arenaName);
    }

    public void cacheArenaMessage(String arenaName) {
        if (arenaName == null) {
            return;
        }
        String raw = this.config.getString("arena." + arenaName + ".message", "&aArena %arena% has been reseted!");
        this.arenaMessageTemplates.put(arenaName.toLowerCase(Locale.ROOT), color(raw));
    }

    public void invalidateArena(String arenaName) {
        if (arenaName != null) {
            this.arenaMessageTemplates.remove(arenaName.toLowerCase(Locale.ROOT));
        }
    }

    public String getConsoleMessage() {
        return this.consoleMessage;
    }

    public String getWandName() {
        return this.wandName;
    }

    public Material getmaterial() {
        return this.wandMaterial;
    }

    public String getpermissionmessage() {
        return this.permissionMessage;
    }

    public List<String> getWandLore() {
        return this.wandLore;
    }

    public String getNotSelectedMessage() {
        return this.notSelectedMessage;
    }

    public String getArenaRemoved(String arenaName) {
        return apply(this.arenaRemovedTemplate, "%arena%", arenaName);
    }

    public String getarenaloadingerror() {
        return this.arenaLoadingError;
    }

    public String getpositioninvalid() {
        return this.positionInvalid;
    }

    public String getworldnotspecified() {
        return this.worldNotSpecified;
    }

    public String getworldnotloaded() {
        return this.worldNotLoaded;
    }

    public String getnamedalready(String arenaName) {
        return apply(this.arenaNamedAlreadyTemplate, "%arena%", arenaName);
    }

    public String getarenasave(String arenaName) {
        return apply(this.arenaSaveTemplate, "%arena%", arenaName);
    }

    public String getarenafailed() {
        return this.arenaFailed;
    }

    public String getpositionnotset() {
        return this.positionNotSet;
    }

    public Location getspawn() {
        return this.getSpawn(null);
    }

    public Location getSpawn(String name) {
        String spawnName = name == null || name.isEmpty() ? this.defaultSpawnName : name;
        String key = spawnName.toLowerCase(Locale.ROOT);
        Location cached = this.spawnCache.get(key);
        if (cached != null) {
            return cached.clone();
        }

        ConfigurationSection section = this.config.getConfigurationSection("spawn." + spawnName);
        if (section != null) {
            Location parsed = this.parseSpawnSection(section);
            if (parsed != null) {
                this.spawnCache.put(key, parsed);
                return parsed.clone();
            }
        }
        if (this.config.isLocation("spawn")) {
            return this.config.getLocation("spawn");
        }
        return null;
    }

    public void invalidateSpawn(String name) {
        if (name == null || name.isEmpty()) {
            this.spawnCache.clear();
            return;
        }
        this.spawnCache.remove(name.toLowerCase(Locale.ROOT));
    }

    public void cacheSpawn(String name) {
        if (name == null || name.isEmpty()) {
            return;
        }
        ConfigurationSection section = this.config.getConfigurationSection("spawn." + name);
        if (section == null) {
            this.spawnCache.remove(name.toLowerCase(Locale.ROOT));
            return;
        }
        Location parsed = this.parseSpawnSection(section);
        if (parsed != null) {
            this.spawnCache.put(name.toLowerCase(Locale.ROOT), parsed);
        }
    }

    private Location parseSpawnSection(ConfigurationSection section) {
        String worldName = section.getString("world");
        List<Double> pos = section.getDoubleList("pos");
        if (worldName == null || pos.size() < 3) {
            return null;
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        return new Location(
                world,
                pos.get(0),
                pos.get(1),
                pos.get(2),
                (float) section.getDouble("yaw", 0.0),
                (float) section.getDouble("pitch", 0.0)
        );
    }

    public String getDefaultSpawnName() {
        return this.defaultSpawnName;
    }

    public String getspawnmessage() {
        return this.playerSpawnMessage;
    }

    public String getspawnsaved(String spawnName) {
        return apply(this.spawnSaveTemplate, "%spawn%", spawnName);
    }

    public String getSpawnNotFound(String spawnName) {
        return apply(this.spawnNotFoundTemplate, "%spawn%", spawnName);
    }

    public String getArenaRemoveFailed(String arenaName) {
        return apply(this.arenaRemoveFailedTemplate, "%arena%", arenaName);
    }

    public String getArenaNotFound() {
        return this.arenaNotFound;
    }

    public String getArenaFolderMissing() {
        return this.arenaFolderMissing;
    }

    public String getPosition1Message(double x, double y, double z) {
        return this.position1Template
                .replace("%pos1_x%", String.valueOf(x))
                .replace("%pos1_y%", String.valueOf(y))
                .replace("%pos1_z%", String.valueOf(z));
    }

    public String getPosition2Message(double x, double y, double z) {
        return this.position2Template
                .replace("%pos2_x%", String.valueOf(x))
                .replace("%pos2_y%", String.valueOf(y))
                .replace("%pos2_z%", String.valueOf(z));
    }

    public void loadBlacklist() {
        Set<String> loaded = new HashSet<>();
        for (String name : this.plugin.getConfig().getStringList("blacklist-Blocks")) {
            loaded.add(normalizeBlockName(name));
        }
        this.blacklisted = loaded;
        this.blacklistEmpty = loaded.isEmpty();
    }

    public boolean isBlacklistEmpty() {
        return this.blacklistEmpty;
    }

    public boolean isBlacklisted(String material) {
        if (this.blacklistEmpty) {
            return false;
        }
        return this.blacklisted.contains(normalizeBlockName(material));
    }

    public static String normalizeBlockName(String material) {
        if (material == null || material.isEmpty()) {
            return "";
        }
        String upper = material.toUpperCase(Locale.ROOT);
        if (upper.startsWith("MINECRAFT:")) {
            return upper.substring("MINECRAFT:".length());
        }
        return upper;
    }

    public String getReloadMessage() {
        return this.reloadMessage;
    }

    public String getMessageType() {
        return this.messageType;
    }

    public int getResetBatchSize() {
        return this.resetBatchSize;
    }

    public long getResetBatchDelay() {
        return this.resetBatchDelay;
    }
}
