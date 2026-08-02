package com.tsotne.fastarenas.utils;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.World;
import com.tsotne.fastarenas.FastArenas;
import java.io.File;
import java.io.FileOutputStream;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class SaveShchem {
    private final FastArenas plugin;

    public SaveShchem(FastArenas plugin) {
        this.plugin = plugin;
    }

    public static void saveSelectionToSchem(FastArenas plugin, Player player, Location pos1, Location pos2, String filename, BuiltInClipboardFormat Format) {
        if (pos1 == null || pos2 == null) {
            String posneeded = plugin.getConfigManager().getpositionnotset();
            SendMessageUtils.Sendmessage(player, posneeded);
        }

        File dir = new File(plugin.getDataFolder(), "arena");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File file = new File(dir, filename + ".schem");
        if (file.exists()) {
            String NamedLikeThat = plugin.getConfigManager().getnamedalready(filename);
            SendMessageUtils.Sendmessage(player, NamedLikeThat);
        } else {
            World weWorld = BukkitAdapter.adapt(player.getWorld());
            BlockVector3 min = BlockVector3.at(
                    Math.min(pos1.getBlockX(), pos2.getBlockX()), Math.min(pos1.getBlockY(), pos2.getBlockY()), Math.min(pos1.getBlockZ(), pos2.getBlockZ())
            );
            BlockVector3 max = BlockVector3.at(
                    Math.max(pos1.getBlockX(), pos2.getBlockX()), Math.max(pos1.getBlockY(), pos2.getBlockY()), Math.max(pos1.getBlockZ(), pos2.getBlockZ())
            );
            CuboidRegion region = new CuboidRegion(weWorld, min, max);
            BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder().world(weWorld).limitUnlimited().build();

                    try {
                        ForwardExtentCopy forwardExtentCopy = new ForwardExtentCopy(editSession, region, clipboard, region.getMinimumPoint());
                        forwardExtentCopy.setCopyingEntities(false);
                        Operations.completeBlindly(forwardExtentCopy);
                    } catch (Throwable var14x) {
                        if (editSession != null) {
                            try {
                                editSession.close();
                            } catch (Throwable var12x) {
                                var14x.addSuppressed(var12x);
                            }
                        }

                        throw var14x;
                    }

                    if (editSession != null) {
                        editSession.close();
                    }
                } catch (Exception var16) {
                    String arenafail = plugin.getConfigManager().getarenafailed();
                    SendMessageUtils.Sendmessage(player, arenafail);
                    return;
                }

                try {
                    ClipboardWriter writer = Format.getWriter(new FileOutputStream(file));

                    try {
                        writer.write(clipboard);
                    } catch (Throwable var13x) {
                        if (writer != null) {
                            try {
                                writer.close();
                            } catch (Throwable var11x) {
                                var13x.addSuppressed(var11x);
                            }
                        }

                        throw var13x;
                    }

                    if (writer != null) {
                        writer.close();
                    }
                } catch (Exception var15) {
                    var15.printStackTrace();
                    return;
                }

                LoadSchem.putCache(filename, clipboard, file.lastModified());

                Bukkit.getScheduler().runTask(plugin, () -> {
                    String arenaSaved = plugin.getConfigManager().getarenasave(filename);
                    SendMessageUtils.Sendmessage(player, arenaSaved + " (" + region.size() + ")");
                });
            });
        }
    }
}