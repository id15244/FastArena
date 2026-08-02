package com.tsotne.fastarenas.utils;

import com.tsotne.fastarenas.FastArenas;
import javax.annotation.Nullable;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class SendMessageUtils {
    private final FastArenas plugin;

    public SendMessageUtils(FastArenas plugin) {
        this.plugin = plugin;
    }

    public static void Sendmessage(@Nullable CommandSender sender, String message) {
        if (message == null) {
            return;
        }
        FastArenas plugin = JavaPlugin.getPlugin(FastArenas.class);
        if (!(sender instanceof Player player)) {
            plugin.getLogger().info(message);
            return;
        }

        String type = plugin.getConfigManager().getMessageType();
        if ("action_bar".equals(type)) {
            player.sendActionBar(message);
        } else {
            player.sendMessage(message);
        }
    }
}
