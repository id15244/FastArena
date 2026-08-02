package com.tsotne.fastarenas.autoreset;

import com.tsotne.fastarenas.FastArenas;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AutoResetManager {
    private final Map<String, AutoResetArena> arenas = new ConcurrentHashMap<>();

    public void registerArena(String id, long delay, long offset, String message, FastArenas plugin) {
        String key = id.toLowerCase();
        if (!this.arenas.containsKey(key)) {
            AutoResetArena arena = new AutoResetArena(id, delay, offset, message);
            arena.start(plugin);
            this.arenas.put(key, arena);
        }
    }

    public void stopArena(String id) {
        AutoResetArena arena = this.arenas.remove(id.toLowerCase());
        if (arena != null) {
            arena.stop();
        }
    }

    public void stopAll() {
        this.arenas.values().forEach(AutoResetArena::stop);
        this.arenas.clear();
    }

    public boolean isRegistered(String id) {
        return this.arenas.containsKey(id.toLowerCase());
    }

    public AutoResetArena getArena(String id) {
        return this.arenas.get(id.toLowerCase());
    }
}
