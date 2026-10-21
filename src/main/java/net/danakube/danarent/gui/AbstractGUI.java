package net.danakube.danarent.gui;

import net.danakube.danarent.DanaRentPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public abstract class AbstractGUI {

    protected final DanaRentPlugin plugin;
    protected final GUIHolder holder;
    protected final Inventory inventory;
    protected final Map<Integer, Consumer<InventoryClickEvent>> slotActions = new HashMap<>();

    public AbstractGUI(DanaRentPlugin plugin, String title, int size) {
        this.plugin = plugin;
        this.holder = new GUIHolder(this);
        Component titleComponent = MiniMessage.miniMessage().deserialize(title);
        Inventory inv;
        try {
            inv = Bukkit.createInventory(holder, size, titleComponent);
        } catch (Throwable t) {
            String legacy = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(titleComponent);
            inv = Bukkit.createInventory(holder, size, legacy);
        }
        this.inventory = inv;
        this.holder.setInventory(inventory);
    }

    protected abstract void initialize();

    public void setItem(int slot, ItemStack item) {
        setItem(slot, item, null);
    }

    public void setItem(int slot, ItemStack item, Consumer<InventoryClickEvent> action) {
        if (slot >= 0 && slot < inventory.getSize()) {
            inventory.setItem(slot, item);
            if (action != null) {
                slotActions.put(slot, action);
            } else {
                slotActions.remove(slot);
            }
        }
    }

    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        Consumer<InventoryClickEvent> action = slotActions.get(slot);
        if (action != null) {
            action.accept(event);
        }
    }

    public void open(Player player) {
        initialize();
        player.openInventory(inventory);
    }

    public Inventory getInventory() {
        return inventory;
    }
}
