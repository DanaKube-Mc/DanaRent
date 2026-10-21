package net.danakube.danarent.wand;

import net.danakube.danarent.util.Cuboid;
import org.bukkit.Location;

public class PlayerSelection {

    private Location pos1;
    private Location pos2;

    public Location getPos1() {
        return pos1;
    }

    public void setPos1(Location pos1) {
        this.pos1 = pos1;
    }

    public Location getPos2() {
        return pos2;
    }

    public void setPos2(Location pos2) {
        this.pos2 = pos2;
    }

    public boolean hasBothPositions() {
        return pos1 != null && pos2 != null;
    }

    public Cuboid toCuboid() {
        if (!hasBothPositions()) {
            throw new IllegalStateException("La sélection est incomplète : les deux coins doivent être définis.");
        }
        if (pos1.getWorld() == null || pos2.getWorld() == null ||
                !pos1.getWorld().getName().equals(pos2.getWorld().getName())) {
            throw new IllegalStateException("Les deux points doivent être situés dans le même monde (mondes différents détectés).");
        }
        return new Cuboid(pos1, pos2);
    }

    public long getVolume() {
        return toCuboid().getVolume();
    }
}
