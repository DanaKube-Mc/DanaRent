package net.danakube.danarent.wand;

import net.danakube.danarent.DanaRentPlugin;
import net.danakube.danarent.config.MessageManager;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Map;

public class WandListener implements Listener {

    private final DanaRentPlugin plugin;
    private final SelectionManager selectionManager;

    public WandListener(DanaRentPlugin plugin, SelectionManager selectionManager) {
        this.plugin = plugin;
        this.selectionManager = selectionManager;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != null && event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        if (!WandFactory.isWand(event.getItem(), plugin)) {
            return;
        }

        Player player = event.getPlayer();
        MessageManager msg = plugin.getMessageManager();

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            Block block = event.getClickedBlock();
            if (block != null) {
                Location loc = block.getLocation();
                PlayerSelection sel = selectionManager.getOrCreateSelection(player.getUniqueId());
                sel.setPos1(loc);

                player.sendMessage(msg.getMessage("wand.point-1-set", Map.of(
                        "x", String.valueOf(loc.getBlockX()),
                        "y", String.valueOf(loc.getBlockY()),
                        "z", String.valueOf(loc.getBlockZ())
                )));
            }
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            Block block = event.getClickedBlock();
            if (block != null) {
                Location loc = block.getLocation();
                PlayerSelection sel = selectionManager.getOrCreateSelection(player.getUniqueId());
                sel.setPos2(loc);

                String volume = sel.hasBothPositions() ? String.valueOf(sel.getVolume()) : "?";
                player.sendMessage(msg.getMessage("wand.point-2-set", Map.of(
                        "x", String.valueOf(loc.getBlockX()),
                        "y", String.valueOf(loc.getBlockY()),
                        "z", String.valueOf(loc.getBlockZ()),
                        "volume", volume
                )));
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        selectionManager.clearSelection(event.getPlayer().getUniqueId());
    }
}
