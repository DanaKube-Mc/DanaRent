package net.danakube.danarent.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class GUIHolder implements InventoryHolder {

    private final AbstractGUI gui;
    private Inventory inventory;

    public GUIHolder(AbstractGUI gui) {
        this.gui = gui;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public AbstractGUI getGUI() {
        return gui;
    }
}
