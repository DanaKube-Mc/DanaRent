package net.danakube.danarent.wand;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SelectionManager {

    private final Map<UUID, PlayerSelection> selections = new ConcurrentHashMap<>();

    public PlayerSelection getSelection(UUID playerUuid) {
        if (playerUuid == null) return null;
        return selections.get(playerUuid);
    }

    public PlayerSelection getOrCreateSelection(UUID playerUuid) {
        if (playerUuid == null) return null;
        return selections.computeIfAbsent(playerUuid, k -> new PlayerSelection());
    }

    public void clearSelection(UUID playerUuid) {
        if (playerUuid != null) {
            selections.remove(playerUuid);
        }
    }
}
