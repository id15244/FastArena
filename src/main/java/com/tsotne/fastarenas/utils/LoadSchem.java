package com.tsotne.fastarenas.utils;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.MaskIntersection;
import com.sk89q.worldedit.function.mask.RegionMask;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.session.PasteBuilder;
import com.tsotne.fastarenas.FastArenas;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;

public class LoadSchem {
    private static final Map<String, CachedClipboard> CACHE = new ConcurrentHashMap<>();
    private static final Set<String> RESETTING = ConcurrentHashMap.newKeySet();
    private static final int DEFAULT_BATCH_SIZE = 16;
    private static final int DEFAULT_BATCH_DELAY = 1;

    private final FastArenas plugin;

    public LoadSchem(FastArenas plugin) {
        this.plugin = plugin;
    }

    private static final class CachedClipboard {
        private final Clipboard clipboard;
        private final long lastModified;

        private CachedClipboard(Clipboard clipboard, long lastModified) {
            this.clipboard = clipboard;
            this.lastModified = lastModified;
        }
    }

    public static void clearCache() {
        CACHE.clear();
    }

    public static void invalidateCache(String arenaName) {
        if (arenaName == null) {
            CACHE.clear();
            return;
        }
        CACHE.remove(arenaName.toLowerCase());
    }

    public static void putCache(String arenaName, Clipboard clipboard, long lastModified) {
        CACHE.put(arenaName.toLowerCase(), new CachedClipboard(clipboard, lastModified));
    }

    public static void loadSchematic(FastArenas plugin, String arenaName, @Nullable Player player) {
        loadSchematic(plugin, arenaName, player, null);
    }

    public static void loadSchematic(FastArenas plugin, String arenaName, @Nullable Player player, @Nullable Runnable onComplete) {
        Runnable work = () -> doLoadSchematic(plugin, arenaName, player, onComplete);
        if (Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, work);
        } else {
            work.run();
        }
    }

    private static void doLoadSchematic(FastArenas plugin, String arenaName, @Nullable Player player, @Nullable Runnable onComplete) {
        String cacheKey = arenaName.toLowerCase();
        if (!RESETTING.add(cacheKey)) {
            return;
        }

        boolean handedOff = false;
        try {
            FileConfiguration config = plugin.getConfig();
            File schemFile = new File(new File(plugin.getDataFolder(), "arena"), arenaName + ".schem");
            if (!schemFile.exists()) {
                Bukkit.broadcastMessage(plugin.getConfigManager().getArenaNotFound());
                return;
            }

            String worldName = config.getString("arena." + arenaName + ".world");
            if (worldName == null) {
                SendMessageUtils.Sendmessage(player, plugin.getConfigManager().getworldnotspecified());
                return;
            }

            World bukkitWorld = Bukkit.getWorld(worldName);
            if (bukkitWorld == null) {
                SendMessageUtils.Sendmessage(player, plugin.getConfigManager().getworldnotloaded());
                return;
            }

            List<Double> pos1List = config.getDoubleList("arena." + arenaName + ".pos1");
            List<Double> pos2List = config.getDoubleList("arena." + arenaName + ".pos2");
            if (pos1List.size() != 3 || pos2List.size() != 3) {
                SendMessageUtils.Sendmessage(player, plugin.getConfigManager().getpositioninvalid());
                return;
            }

            double minX = Math.min(pos1List.get(0), pos2List.get(0));
            double minY = Math.min(pos1List.get(1), pos2List.get(1));
            double minZ = Math.min(pos1List.get(2), pos2List.get(2));

            Clipboard clipboard = getOrLoadClipboard(plugin, arenaName, schemFile, player);
            if (clipboard == null) {
                return;
            }

            boolean items = config.getBoolean("arena." + arenaName + ".clearitems", false);
            boolean crystal = config.getBoolean("arena." + arenaName + ".clearcrystal", false);
            boolean players = config.getBoolean("arena." + arenaName + ".tpplayers", false);
            boolean tpUpSafe = config.getBoolean("arena." + arenaName + ".tpupsafe", false);
            String spawnName = config.getString(
                    "arena." + arenaName + ".spawn",
                    config.getString("default-spawn", "default")
            );

            int configuredBatchSize = plugin.getConfigManager().getResetBatchSize();
            long configuredBatchDelay = plugin.getConfigManager().getResetBatchDelay();
            final int batchSize = configuredBatchSize < 1 ? DEFAULT_BATCH_SIZE : configuredBatchSize;
            final long batchDelay = configuredBatchDelay < 0L ? DEFAULT_BATCH_DELAY : configuredBatchDelay;

            BlockVector3 pasteTo = BlockVector3.at(minX, minY, minZ);
            com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(bukkitWorld);
            List<CuboidRegion> batches = buildBatches(clipboard.getRegion(), batchSize);

            Runnable startPaste = () -> pasteBatches(
                    plugin,
                    player,
                    cacheKey,
                    clipboard,
                    weWorld,
                    pasteTo,
                    batches,
                    0,
                    batchDelay,
                    bukkitWorld,
                    pos1List,
                    pos2List,
                    items,
                    crystal,
                    players,
                    tpUpSafe,
                    spawnName,
                    onComplete
            );

            handedOff = true;
            if (tpUpSafe) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    teleportPlayersUp(bukkitWorld, pos1List, pos2List);
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, startPaste);
                });
            } else {
                startPaste.run();
            }
        } finally {
            if (!handedOff) {
                RESETTING.remove(cacheKey);
            }
        }
    }

    private static List<CuboidRegion> buildBatches(Region region, int batchSize) {
        List<CuboidRegion> batches = new ArrayList<>();
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();

        for (int x = min.getBlockX(); x <= max.getBlockX(); x += batchSize) {
            for (int z = min.getBlockZ(); z <= max.getBlockZ(); z += batchSize) {
                int maxBatchX = Math.min(x + batchSize - 1, max.getBlockX());
                int maxBatchZ = Math.min(z + batchSize - 1, max.getBlockZ());
                batches.add(new CuboidRegion(
                        BlockVector3.at(x, min.getBlockY(), z),
                        BlockVector3.at(maxBatchX, max.getBlockY(), maxBatchZ)
                ));
            }
        }
        return batches;
    }

    private static void pasteBatches(
            FastArenas plugin,
            @Nullable Player player,
            String cacheKey,
            Clipboard clipboard,
            com.sk89q.worldedit.world.World weWorld,
            BlockVector3 pasteTo,
            List<CuboidRegion> batches,
            int index,
            long batchDelay,
            World bukkitWorld,
            List<Double> pos1List,
            List<Double> pos2List,
            boolean items,
            boolean crystal,
            boolean players,
            boolean tpUpSafe,
            String spawnName,
            @Nullable Runnable onComplete
    ) {
        if (index >= batches.size()) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                try {
                    clear(bukkitWorld, pos1List, pos2List, items, crystal, players, tpUpSafe, spawnName);
                    if (onComplete != null) {
                        onComplete.run();
                    }
                } finally {
                    RESETTING.remove(cacheKey);
                }
            });
            return;
        }

        CuboidRegion batch = batches.get(index);
        try {
            EditSession editSession = WorldEdit.getInstance()
                    .newEditSessionBuilder()
                    .world(weWorld)
                    .fastMode(true)
                    .limitUnlimited()
                    .build();

            try {
                editSession.disableHistory();
                Mask batchMask = createBatchMask(plugin, clipboard, batch);
                PasteBuilder pasteBuilder = new ClipboardHolder(clipboard)
                        .createPaste(editSession)
                        .to(pasteTo)
                        .ignoreAirBlocks(false)
                        .copyEntities(false)
                        .maskSource(batchMask);

                Operations.complete(pasteBuilder.build());
            } finally {
                editSession.close();
            }
        } catch (WorldEditException ex) {
            SendMessageUtils.Sendmessage(player, plugin.getConfigManager().getarenaloadingerror());
            RESETTING.remove(cacheKey);
            return;
        }

        Runnable next = () -> pasteBatches(
                plugin,
                player,
                cacheKey,
                clipboard,
                weWorld,
                pasteTo,
                batches,
                index + 1,
                batchDelay,
                bukkitWorld,
                pos1List,
                pos2List,
                items,
                crystal,
                players,
                tpUpSafe,
                spawnName,
                onComplete
        );

        if (batchDelay <= 0L) {
            next.run();
        } else {
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, next, batchDelay);
        }
    }

    private static Mask createBatchMask(FastArenas plugin, Clipboard clipboard, CuboidRegion batch) {
        Mask regionMask = new RegionMask(batch);
        if (plugin.getConfigManager().isBlacklistEmpty()) {
            return regionMask;
        }
        return new MaskIntersection(regionMask, createBlacklistMask(plugin, clipboard));
    }

    private static Mask createBlacklistMask(FastArenas plugin, Clipboard clipboard) {
        return new Mask() {
            @Override
            public boolean test(BlockVector3 vector) {
                return !plugin.getConfigManager().isBlacklisted(clipboard.getBlock(vector).getBlockType().getName());
            }

            @Override
            public Mask copy() {
                return this;
            }
        };
    }

    @Nullable
    private static Clipboard getOrLoadClipboard(FastArenas plugin, String arenaName, File schemFile, @Nullable Player player) {
        long lastModified = schemFile.lastModified();
        String cacheKey = arenaName.toLowerCase();
        CachedClipboard cached = CACHE.get(cacheKey);
        if (cached != null && cached.lastModified == lastModified) {
            return cached.clipboard;
        }

        ClipboardFormat format = ClipboardFormats.findByFile(schemFile);
        if (format == null) {
            SendMessageUtils.Sendmessage(player, plugin.getConfigManager().getArenaNotFound());
            return null;
        }

        try (ClipboardReader reader = format.getReader(new FileInputStream(schemFile))) {
            Clipboard clipboard = reader.read();
            CACHE.put(cacheKey, new CachedClipboard(clipboard, lastModified));
            return clipboard;
        } catch (IOException ex) {
            SendMessageUtils.Sendmessage(player, plugin.getConfigManager().getarenaloadingerror());
            return null;
        }
    }

    private static void teleportPlayersUp(World world, List<Double> start, List<Double> end) {
        if (world == null || start == null || end == null || start.size() < 3 || end.size() < 3) {
            return;
        }

        double minX = Math.min(start.get(0), end.get(0));
        double minY = Math.min(start.get(1), end.get(1));
        double minZ = Math.min(start.get(2), end.get(2));
        double maxX = Math.max(start.get(0), end.get(0));
        double maxY = Math.max(start.get(1), end.get(1));
        double maxZ = Math.max(start.get(2), end.get(2));
        BoundingBox box = BoundingBox.of(new Location(world, minX, minY, minZ), new Location(world, maxX, maxY, maxZ));
        int searchMinY = (int) Math.floor(minY);
        int searchMaxY = (int) Math.floor(maxY);

        for (Entity entity : world.getNearbyEntities(box)) {
            if (entity.getType() != EntityType.PLAYER) {
                continue;
            }
            entity.teleport(findSafeLocation(world, entity.getLocation(), searchMinY, searchMaxY));
        }
    }

    /**
     * Scans from arena top downward and returns the first safe stand location
     * (solid ground below, passable feet + head). Falls back to maxY + 1.
     */
    private static Location findSafeLocation(World world, Location from, int minY, int maxY) {
        int x = from.getBlockX();
        int z = from.getBlockZ();
        int top = Math.min(maxY, world.getMaxHeight() - 2);
        int bottom = Math.max(minY + 1, world.getMinHeight() + 1);

        for (int y = top; y >= bottom; y--) {
            if (isSafeStand(world, x, y, z)) {
                Location safe = from.clone();
                safe.setX(x + 0.5);
                safe.setY(y);
                safe.setZ(z + 0.5);
                return safe;
            }
        }

        Location fallback = from.clone();
        fallback.setY(Math.min(maxY + 1.0, world.getMaxHeight() - 1.0));
        return fallback;
    }

    private static boolean isSafeStand(World world, int x, int y, int z) {
        Block ground = world.getBlockAt(x, y - 1, z);
        Block feet = world.getBlockAt(x, y, z);
        Block head = world.getBlockAt(x, y + 1, z);
        Material groundType = ground.getType();
        Material feetType = feet.getType();
        Material headType = head.getType();

        if (!groundType.isSolid()) {
            return false;
        }
        if (groundType == Material.CACTUS || groundType == Material.MAGMA_BLOCK || groundType == Material.CAMPFIRE || groundType == Material.SOUL_CAMPFIRE) {
            return false;
        }
        if (!feet.isPassable() || !head.isPassable()) {
            return false;
        }
        return feetType != Material.LAVA
                && feetType != Material.FIRE
                && feetType != Material.SOUL_FIRE
                && headType != Material.LAVA
                && headType != Material.FIRE
                && headType != Material.SOUL_FIRE;
    }

    private static void clear(
            World world,
            List<Double> start,
            List<Double> end,
            boolean items,
            boolean crystal,
            boolean players,
            boolean tpUpSafe,
            String spawnName
    ) {
        if (!items && !crystal && !players && !tpUpSafe) {
            return;
        }
        if (world == null || start == null || end == null || start.size() < 3 || end.size() < 3) {
            return;
        }

        double minX = Math.min(start.get(0), end.get(0));
        double minY = Math.min(start.get(1), end.get(1));
        double minZ = Math.min(start.get(2), end.get(2));
        double maxX = Math.max(start.get(0), end.get(0));
        double maxY = Math.max(start.get(1), end.get(1));
        double maxZ = Math.max(start.get(2), end.get(2));
        BoundingBox box = BoundingBox.of(new Location(world, minX, minY, minZ), new Location(world, maxX, maxY, maxZ));
        FastArenas fastArenas = JavaPlugin.getPlugin(FastArenas.class);
        Location spawn = players ? fastArenas.getConfigManager().getSpawn(spawnName) : null;
        String message = players ? fastArenas.getConfigManager().getspawnmessage() : null;
        int searchMinY = (int) Math.floor(minY);
        int searchMaxY = (int) Math.floor(maxY);

        for (Entity entity : world.getNearbyEntities(box)) {
            if (items && entity instanceof Item) {
                entity.remove();
                continue;
            }
            if (crystal && entity.getType() == EntityType.ENDER_CRYSTAL) {
                entity.remove();
            }
            if (entity.getType() != EntityType.PLAYER) {
                continue;
            }
            if (tpUpSafe) {
                entity.teleport(findSafeLocation(world, entity.getLocation(), searchMinY, searchMaxY));
            } else if (players && spawn != null) {
                entity.teleport(spawn);
                entity.sendMessage(message);
            }
        }
    }
}
