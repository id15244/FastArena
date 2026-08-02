package com.tsotne.fastarenas.commands;

import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.tsotne.fastarenas.FastArenas;
import com.tsotne.fastarenas.Event.ArenaCreate;
import com.tsotne.fastarenas.utils.Color;
import com.tsotne.fastarenas.utils.LoadSchem;
import com.tsotne.fastarenas.utils.SaveShchem;
import com.tsotne.fastarenas.utils.SendMessageUtils;
import com.tsotne.fastarenas.utils.getitem;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TextComponent.Builder;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Command implements CommandExecutor, TabExecutor {
    private final FastArenas plugin;
    private static final Map<String, String> PERMS = new HashMap();

    public Command(FastArenas plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        Player player = sender instanceof Player ? (Player)sender : null;
        String permMessage = this.plugin.getConfigManager().getpermissionmessage();
        if (args.length == 0) {
            SendMessageUtils.Sendmessage(sender, Color.translateColors("&c/fa <subcommand>"));
            return true;
        } else {
            String var7 = args[0].toLowerCase();
            switch (var7) {
                case "create":
                    if (player == null) {
                        SendMessageUtils.Sendmessage(sender, this.plugin.getConfigManager().getConsoleMessage());
                        return true;
                    } else if (!player.hasPermission("fastarena.create") && !player.hasPermission("fastarena.admin")) {
                        SendMessageUtils.Sendmessage(player, permMessage);
                        return true;
                    } else if (args.length < 2) {
                        SendMessageUtils.Sendmessage(player, "§cUsage: /fa create <arena> [format]");
                        return true;
                    } else {
                        if (ArenaCreate.pos1 != null && ArenaCreate.pos2 != null) {
                            String name = args[1];
                            BuiltInClipboardFormat format = BuiltInClipboardFormat.FAST_V3;
                            if (args.length >= 3) {
                                try {
                                    format = BuiltInClipboardFormat.valueOf(args[2].toUpperCase());
                                } catch (Exception var13) {
                                }
                            }

                            this.plugin.getConfig().set("arena." + name + ".name", name);
                            this.plugin.getConfig().set("arena." + name + ".world", ArenaCreate.pos2.getWorld().getName());
                            this.plugin.getConfig().set("arena." + name + ".message", "&aArena %arena% has been reset!");
                            this.plugin.getConfig().set("arena." + name + ".pos1", List.of(ArenaCreate.pos1.x(), ArenaCreate.pos1.y(), ArenaCreate.pos1.z()));
                            this.plugin.getConfig().set("arena." + name + ".pos2", List.of(ArenaCreate.pos2.x(), ArenaCreate.pos2.y(), ArenaCreate.pos2.z()));
                            this.plugin.getConfig().set("arena." + name + ".clearitems", true);
                            this.plugin.getConfig().set("arena." + name + ".clearcrystal", true);
                            this.plugin.getConfig().set("arena." + name + ".tpplayers", false);
                            this.plugin.getConfig().set("arena." + name + ".tpupsafe", false);
                            this.plugin.getConfig().set("arena." + name + ".spawn", this.plugin.getConfigManager().getDefaultSpawnName());
                            this.plugin.getConfig().set("arena." + name + ".enabled", true);
                            this.plugin.getConfig().set("arena." + name + ".offset", 0);
                            this.plugin.getConfig().set("arena." + name + ".autoreset", 0);
                            this.plugin.saveConfig();
                            SaveShchem.saveSelectionToSchem(this.plugin, player, ArenaCreate.pos1, ArenaCreate.pos2, name, format);
                            return true;
                        }

                        SendMessageUtils.Sendmessage(player, this.plugin.getConfigManager().getNotSelectedMessage());
                        return true;
                    }
                case "reset":
                    if (player != null && !player.hasPermission("fastarena.reset") && !player.hasPermission("fastarena.admin")) {
                        SendMessageUtils.Sendmessage(player, permMessage);
                        return true;
                    } else {
                        if (args.length < 2) {
                            SendMessageUtils.Sendmessage(player, "§cUsage: /fa reset <arena>");
                            return true;
                        }

                        String arenaName = args[1];
                        LoadSchem.loadSchematic(this.plugin, arenaName, player, () -> {
                            String msg = this.plugin.getConfigManager().getarenamessage(arenaName);
                            if (!msg.isEmpty()) {
                                Bukkit.broadcastMessage(msg);
                            }
                        });

                        return true;
                    }
                case "list":
                    if (player == null) {
                        SendMessageUtils.Sendmessage(sender, this.plugin.getConfigManager().getConsoleMessage());
                        return true;
                    } else {
                        if (!player.hasPermission("fastarena.list") && !player.hasPermission("fastarena.admin")) {
                            SendMessageUtils.Sendmessage(player, permMessage);
                            return true;
                        }

                        SendMessageUtils.Sendmessage(player, Color.translateColors("&6------------------- Arenas -------------------"));
                        this.listYmlFiles(player);
                        return true;
                    }
                case "remove":
                    if (player != null && !player.hasPermission("fastarena.remove") && !player.hasPermission("fastarena.admin")) {
                        SendMessageUtils.Sendmessage(player, permMessage);
                        return true;
                    } else if (args.length < 2) {
                        SendMessageUtils.Sendmessage(player, "§cUsage: /fa remove <arena>");
                        return true;
                    } else {
                        String name = args[1];
                        this.plugin.getautoResetManager().stopArena(name);
                        LoadSchem.invalidateCache(name);
                        this.plugin.getConfig().set("arena." + name, null);
                        this.plugin.saveConfig();
                        File schem = new File(new File(this.plugin.getDataFolder(), "arena"), name + ".schem");
                        if (schem.exists() && schem.delete()) {
                            SendMessageUtils.Sendmessage(player, this.plugin.getConfigManager().getArenaRemoved(name));
                        } else {
                            SendMessageUtils.Sendmessage(player, this.plugin.getConfigManager().getArenaRemoveFailed(name));
                        }

                        return true;
                    }
                case "setspawn":
                    if (player == null) {
                        SendMessageUtils.Sendmessage(sender, this.plugin.getConfigManager().getConsoleMessage());
                        return true;
                    } else {
                        if (!player.hasPermission("fastarena.setspawn") && !player.hasPermission("fastarena.admin")) {
                            SendMessageUtils.Sendmessage(player, permMessage);
                            return true;
                        }

                        String spawnName = args.length >= 2 ? args[1] : this.plugin.getConfigManager().getDefaultSpawnName();
                        Location location = player.getLocation();
                        String basePath = "spawn." + spawnName;
                        this.plugin.getConfig().set(basePath + ".world", location.getWorld().getName());
                        this.plugin.getConfig().set(basePath + ".pos", List.of(location.getX(), location.getY(), location.getZ()));
                        this.plugin.getConfig().set(basePath + ".yaw", location.getYaw());
                        this.plugin.getConfig().set(basePath + ".pitch", location.getPitch());
                        SendMessageUtils.Sendmessage(player, this.plugin.getConfigManager().getspawnsaved(spawnName));
                        this.plugin.saveConfig();
                        return true;
                    }
                case "wand":
                    if (player == null) {
                        SendMessageUtils.Sendmessage(sender, this.plugin.getConfigManager().getConsoleMessage());
                        return true;
                    } else {
                        if (!player.hasPermission("fastarena.wand") && !player.hasPermission("fastarena.admin")) {
                            SendMessageUtils.Sendmessage(player, permMessage);
                            return true;
                        }

                        ItemStack wand = getitem.getItemWithNameandlores(
                                new ItemStack(this.plugin.getConfigManager().getmaterial()),
                                this.plugin.getConfigManager().getWandName(),
                                this.plugin.getConfigManager().getWandLore()
                        );
                        player.getInventory().addItem(new ItemStack[]{wand});
                        return true;
                    }
                case "reload":
                    if (player != null && !player.hasPermission("fastarena.reload") && !player.hasPermission("fastarena.admin")) {
                        SendMessageUtils.Sendmessage(player, permMessage);
                        return true;
                    }

                    this.plugin.reloadEverything();
                    SendMessageUtils.Sendmessage(player, this.plugin.getConfigManager().getReloadMessage());
                    return true;
                case "start":
                    if (player != null && !player.hasPermission("fastarena.start") && !player.hasPermission("fastarena.admin")) {
                        SendMessageUtils.Sendmessage(player, permMessage);
                        return true;
                    } else {
                        if (args.length < 2) {
                            SendMessageUtils.Sendmessage(player, "§cUsage: /fa start <arena>");
                            return true;
                        }

                        this.startArena(args[1], player);
                        return true;
                    }
                case "stop":
                    if (player != null && !sender.hasPermission("fastarena.stop") && !sender.hasPermission("fastarena.admin")) {
                        SendMessageUtils.Sendmessage(sender, permMessage);
                        return true;
                    } else {
                        if (args.length < 2) {
                            SendMessageUtils.Sendmessage(sender, "§cUsage: /fa stop <arena>");
                            return true;
                        }

                        if (this.plugin.getautoResetManager().isRegistered(args[1])) {
                            this.plugin.getautoResetManager().stopArena(args[1]);
                            SendMessageUtils.Sendmessage(sender, "§aStopped arena §e" + args[1]);
                        } else {
                            SendMessageUtils.Sendmessage(sender, "§cArena §e" + args[1] + " §cnot running.");
                        }

                        return true;
                    }
                case "stopall":
                    if (player != null && !sender.hasPermission("fastarena.stop") && !sender.hasPermission("fastarena.admin")) {
                        SendMessageUtils.Sendmessage(sender, permMessage);
                        return true;
                    }

                    this.plugin.getautoResetManager().stopAll();
                    return true;
                case "autoreset":
                    if (player != null && !player.hasPermission("fastarena.autoreset") && !player.hasPermission("fastarena.admin")) {
                        SendMessageUtils.Sendmessage(player, permMessage);
                        return true;
                    } else {
                        if (args.length < 3) {
                            SendMessageUtils.Sendmessage(player, "§cUsage: /fa autoreset <arena> <seconds>");
                            return true;
                        }

                        try {
                            int seconds = Integer.parseInt(args[2]);
                            this.plugin.getConfig().set("arena." + args[1] + ".autoreset", seconds);
                            this.plugin.getConfig().set("arena." + args[1] + ".enabled", true);
                            this.plugin.saveConfig();
                            this.startArena(args[1], player);
                        } catch (Exception var12) {
                            SendMessageUtils.Sendmessage(player, ChatColor.RED + "Error");
                        }

                        return true;
                    }
                default:
                    return true;
            }
        }
    }

    public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command cmd, String alias, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("fastarena")) {
            return Collections.emptyList();
        } else if (args.length == 1) {
            return (List<String>)PERMS.entrySet()
                    .stream()
                    .filter(e -> sender.hasPermission((String)e.getValue()) || sender.hasPermission("fastarena.admin"))
                    .map(Entry::getKey)
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("setspawn")) {
            String prefix = args[1].toLowerCase();
            ConfigurationSection spawns = this.plugin.getConfig().getConfigurationSection("spawn");
            if (spawns == null) {
                return Collections.emptyList();
            }
            return spawns.getKeys(false).stream().filter(name -> name.startsWith(prefix)).collect(Collectors.toList());
        } else if (args.length == 2 && List.of("reset", "remove", "stop", "start", "autoreset").contains(args[0].toLowerCase())) {
            File dir = new File(this.plugin.getDataFolder(), "arena");
            return !dir.exists()
                    ? Collections.emptyList()
                    : (List)Arrays.stream((File[])Objects.requireNonNull(dir.listFiles(f -> f.getName().endsWith(".schem"))))
                    .map(f -> f.getName().replace(".schem", ""))
                    .filter(n -> n.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        } else {
            return args.length == 3 && args[0].equalsIgnoreCase("create")
                    ? (List)Arrays.stream(BuiltInClipboardFormat.values())
                    .map(Enum::name)
                    .filter(s -> !Set.of("BROKENENTITY", "MCEDIT_SCHEMATIC", "MINECRAFT_STRUCTURE", "PNG").contains(s))
                    .collect(Collectors.toList())
                    : Collections.emptyList();
        }
    }

    public void listYmlFiles(Player sender) {
        File dir = new File(this.plugin.getDataFolder(), "arena");
        int i = 1;
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".schem")) {
                        String name = file.getName().substring(0, file.getName().length() - 6);
                        List<String> pos1 = this.plugin.getConfig().getStringList("arena." + name + ".pos1");
                        List<String> pos2 = this.plugin.getConfig().getStringList("arena." + name + ".pos2");
                        if (pos1.size() == 3 && pos2.size() == 3) {
                            int x1 = (int)Double.parseDouble((String)pos1.get(0));
                            int y1 = (int)Double.parseDouble((String)pos1.get(1));
                            int z1 = (int)Double.parseDouble((String)pos1.get(2));
                            int x2 = (int)Double.parseDouble((String)pos2.get(0));
                            int y2 = (int)Double.parseDouble((String)pos2.get(1));
                            int z2 = (int)Double.parseDouble((String)pos2.get(2));
                            int minX = (x1 + x2) / 2;
                            int minY = (y1 + y2) / 2;
                            int minZ = (z1 + z2) / 2;
                            Component message = ((Builder)((Builder)((Builder)((Builder)Component.text().append(Component.text(i + ". ", NamedTextColor.AQUA)))
                                    .append(Component.text(name, NamedTextColor.GOLD)))
                                    .append(Component.space()))
                                    .append(
                                            ((TextComponent)Component.text("[TP]", NamedTextColor.GRAY).hoverEvent(HoverEvent.showText(Component.text("Click to teleport!"))))
                                                    .clickEvent(ClickEvent.runCommand("/teleport " + sender.getName() + " " + minX + " " + minY + " " + minZ))
                                    ))
                                    .build();
                            sender.sendMessage(message);
                            i++;
                        } else {
                            sender.sendMessage(Component.text("Invalid position data for arena: " + name, NamedTextColor.RED));
                        }
                    }
                }
            } else {
                String arenanotfound = this.plugin.getConfigManager().getArenaNotFound();
                sender.sendMessage(arenanotfound);
            }
        } else {
            String arenanotfound = this.plugin.getConfigManager().getArenaFolderMissing();
            sender.sendMessage(arenanotfound);
        }
    }

    private void startArena(String name, Player player) {
        ConfigurationSection s = this.plugin.getConfig().getConfigurationSection("arena." + name);
        if (s == null) {
            SendMessageUtils.Sendmessage(player, "§cArena §e" + name + " §cnot found.");
        } else if (!s.getBoolean("enabled")) {
            SendMessageUtils.Sendmessage(player, "§cArena disabled.");
        } else {
            long auto = s.getLong("autoreset");
            if (auto <= 0L) {
                SendMessageUtils.Sendmessage(player, "§cInvalid autoreset.");
            } else if (this.plugin.getautoResetManager().isRegistered(name)) {
                SendMessageUtils.Sendmessage(player, "§eArena already running.");
            } else {
                this.plugin.getautoResetManager().registerArena(name, auto * 20L, s.getLong("offset") * 20L, s.getString("message"), this.plugin);
                SendMessageUtils.Sendmessage(player, "§aStarted arena §e" + name);
            }
        }
    }

    static {
        PERMS.put("wand", "fastarena.wand");
        PERMS.put("create", "fastarena.create");
        PERMS.put("reset", "fastarena.reset");
        PERMS.put("autoreset", "fastarena.autoreset");
        PERMS.put("list", "fastarena.list");
        PERMS.put("reload", "fastarena.reload");
        PERMS.put("remove", "fastarena.remove");
        PERMS.put("stop", "fastarena.stop");
        PERMS.put("stopall", "fastarena.stop");
        PERMS.put("start", "fastarena.start");
        PERMS.put("setspawn", "fastarena.setspawn");
    }
}