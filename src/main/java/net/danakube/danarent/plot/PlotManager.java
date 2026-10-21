package net.danakube.danarent.plot;

import net.danakube.danarent.database.PlotRepository;
import net.danakube.danarent.model.RentPlot;
import net.danakube.danarent.util.Cuboid;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class PlotManager {

    private final PlotRepository plotRepository;
    private final Map<Integer, RentPlot> plotsById = new ConcurrentHashMap<>();

    public PlotManager(PlotRepository plotRepository) {
        this.plotRepository = plotRepository;
    }

    public void addPlot(RentPlot plot) {
        if (plot != null && plot.getId() > 0) {
            plotsById.put(plot.getId(), plot);
        }
    }

    public void removePlot(int plotId) {
        plotsById.remove(plotId);
    }

    public Optional<RentPlot> getPlot(int plotId) {
        return Optional.ofNullable(plotsById.get(plotId));
    }

    public Collection<RentPlot> getAllPlots() {
        return plotsById.values();
    }

    public Optional<RentPlot> getPlotByName(String name, UUID ownerUuid) {
        if (name == null || ownerUuid == null) return Optional.empty();
        return plotsById.values().stream()
                .filter(p -> p.getOwnerUuid().equals(ownerUuid) && p.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    public Optional<RentPlot> getPlotAt(Location loc) {
        if (loc == null || loc.getWorld() == null) return Optional.empty();
        return getPlotAt(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    public Optional<RentPlot> getPlotAt(String world, int x, int y, int z) {
        return plotsById.values().stream()
                .filter(p -> p.getCuboid().contains(world, x, y, z))
                .findFirst();
    }

    public boolean isOverlapping(Cuboid cuboid) {
        if (cuboid == null) return false;
        for (RentPlot plot : plotsById.values()) {
            if (plot.getCuboid().overlaps(cuboid)) {
                return true;
            }
        }
        return false;
    }

    public int getPlotCountForIsland(String islandId) {
        if (islandId == null) return 0;
        int count = 0;
        for (RentPlot plot : plotsById.values()) {
            if (islandId.equals(plot.getIslandId())) {
                count++;
            }
        }
        return count;
    }

    public List<RentPlot> getPlotsForIsland(String islandId) {
        List<RentPlot> list = new ArrayList<>();
        if (islandId == null) return list;
        for (RentPlot plot : plotsById.values()) {
            if (islandId.equals(plot.getIslandId())) {
                list.add(plot);
            }
        }
        return list;
    }

    public List<RentPlot> getListedPlots() {
        List<RentPlot> list = new ArrayList<>();
        for (RentPlot plot : plotsById.values()) {
            if (plot.isListed()) {
                list.add(plot);
            }
        }
        return list;
    }

    public CompletableFuture<Void> reloadCache() {
        return plotRepository.findListedPlots().thenAccept(plots -> {
            plotsById.clear();
            for (RentPlot p : plots) {
                plotsById.put(p.getId(), p);
            }
        });
    }
}
