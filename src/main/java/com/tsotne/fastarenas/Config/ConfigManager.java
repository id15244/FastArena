package com.tsotne.fastarenas.Config;

import com.tsotne.fastarenas.FastArenas;
import com.tsotne.fastarenas.utils.Color;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {
    private final FastArenas plugin;
    private FileConfiguration config;
    private Set<String> blacklisted;

    public ConfigManager(FastArenas plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        this.loadBlacklist();
    }

    public void reload() {
        this.plugin.reloadConfig();
        this.config = this.plugin.getConfig();
        this.loadBlacklist();
    }

    private String color(String msg) {
        return Color.translateColors(msg);
    }

    private List<String> colorList(List<String> list) {
        return list.stream().map(this::color).toList();
    }

    public String getarenamessage(String Filename) {
        return this.color(this.config.getString("arena." + Filename + ".message", "&aArena %arena% has been reseted!")).replace("%arena%", Filename);
    }

    public String getConsoleMessage() {
        return this.color(this.config.getString("console-message", "&cOnly players can use this command."));
    }

    public String getWandName() {
        return this.color(this.config.getString("wand-name", "&6ᴡᴀɴᴅ"));
    }

    public Material getmaterial() {
        return Material.valueOf(this.config.getString("wand-material", "STICK").toUpperCase());
    }

    public String getpermissionmessage() {
        return this.color(this.config.getString("permission", "You Don't have Permission To use this command"));
    }

    public List<String> getWandLore() {
        return this.colorList(this.config.getStringList("wand-lore"));
    }

    public String getNotSelectedMessage() {
        return this.color(this.config.getString("not-selected", "&cYou must select both positions."));
    }

    public String getArenaRemoved(String arenaName) {
        return this.color(this.config.getString("arena-removed", "&aArena %arena% removed.")).replace("%arena%", arenaName);
    }

    public String getarenaloadingerror() {
        return this.color(this.config.getString("arena-loading-error", "§cError loading Arena"));
    }

    public String getpositioninvalid() {
        return this.color(this.config.getString("position-invalid", "§cInvalid pos1/pos2 in config for arena"));
    }

    public String getworldnotspecified() {
        return this.color(this.config.getString("world-not-specified", "§cWorld not specified for arena"));
    }

    public String getworldnotloaded() {
        return this.color(this.config.getString("world-not-loaded", "§cWorld not loaded"));
    }

    public String getnamedalready(String arenaName) {
        return this.color(this.config.getString("arena-named-already", "§cThere is already an arena named like that %arena%")).replace("%arena%", arenaName);
    }

    public String getarenasave(String arenaName) {
        return this.color(this.config.getString("arena-save", "Arena saved as %arena%.schem")).replaceAll("%arena%", arenaName);
    }

    public String getarenafailed() {
        return this.color(this.config.getString("arena-failed", "§cFailed to save Arena"));
    }

    public String getpositionnotset() {
        return this.color(this.config.getString("position-not-set", "§cYou need to select both positions!"));
    }

    public Location getspawn() {
        return this.getSpawn(null);
    }

    public Location getSpawn(String name) {
        String spawnName = name == null || name.isEmpty()
                ? this.config.getString("default-spawn", "default")
                : name;
        ConfigurationSection section = this.config.getConfigurationSection("spawn." + spawnName);
        if (section != null) {
            return this.parseSpawnSection(section);
        }
        if (this.config.isLocation("spawn")) {
            return this.config.getLocation("spawn");
        }
        return null;
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
        return this.config.getString("default-spawn", "default");
    }

    public String getspawnmessage() {
        return this.color(this.config.getString("player-spawn-message", "§cYou have been Teleported to spawn!"));
    }

    public String getspawnsaved(String spawnName) {
        return this.color(this.config.getString("spawn-save", "§aSuccessfully saved spawn %spawn%!"))
                .replace("%spawn%", spawnName);
    }

    public String getSpawnNotFound(String spawnName) {
        return this.color(this.config.getString("spawn-not-found", "§cSpawn point %spawn% not found."))
                .replace("%spawn%", spawnName);
    }

    public String getArenaRemoveFailed(String arenaName) {
        return this.color(this.config.getString("arena-remove-failed", "&cFailed to delete schematic for %arena%."))
                .replace("%arena%", arenaName);
    }

    public String getArenaNotFound() {
        return this.color(this.config.getString("arena-not-found", "&cArena not found."));
    }

    public String getArenaFolderMissing() {
        return this.color(this.config.getString("arena-folder-missing", "&cArena folder does not exist."));
    }

    public String getPosition1Message(double x, double y, double z) {
        return this.color(this.config.getString("position-1-set", "&6Position 1 set ( %pos1_x% %pos1_y% %pos1_z% )"))
                .replace("%pos1_x%", String.valueOf(x))
                .replace("%pos1_y%", String.valueOf(y))
                .replace("%pos1_z%", String.valueOf(z));
    }

    public String getPosition2Message(double x, double y, double z) {
        return this.color(this.config.getString("position-2-set", "&6Position 2 set ( %pos2_x% %pos2_y% %pos2_z% )"))
                .replace("%pos2_x%", String.valueOf(x))
                .replace("%pos2_y%", String.valueOf(y))
                .replace("%pos2_z%", String.valueOf(z));
    }

    public void loadBlacklist() {
        this.blacklisted = this.plugin
                .getConfig()
                .getStringList("blacklist-Blocks")
                .stream()
                .map(ConfigManager::normalizeBlockName)
                .collect(Collectors.toSet());
    }

    public boolean isBlacklistEmpty() {
        return this.blacklisted == null || this.blacklisted.isEmpty();
    }

    public boolean isBlacklisted(String material) {
        if (this.blacklisted == null || this.blacklisted.isEmpty()) {
            return false;
        }
        return this.blacklisted.contains(normalizeBlockName(material));
    }

    public static String normalizeBlockName(String material) {
        if (material == null) {
            return "";
        }
        String upper = material.toUpperCase(Locale.ROOT);
        if (upper.startsWith("MINECRAFT:")) {
            return upper.substring("MINECRAFT:".length());
        }
        return upper;
    }

    public String getReloadMessage() {
        return this.color(this.config.getString("reload-complete", "&aFastArenas Reloaded."));
    }
}