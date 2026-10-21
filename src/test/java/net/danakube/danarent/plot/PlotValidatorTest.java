package net.danakube.danarent.plot;

import net.danakube.danarent.config.ConfigManager;
import net.danakube.danarent.database.PlotRepository;
import net.danakube.danarent.hook.SkyblockHook;
import net.danakube.danarent.model.PlotType;
import net.danakube.danarent.model.RentPlot;
import net.danakube.danarent.util.Cuboid;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlotValidatorTest {

    private PlotRepository plotRepository;
    private PlotManager plotManager;
    private SkyblockHook skyblockHook;
    private ConfigManager configManager;
    private PlotValidator validator;

    @BeforeEach
    void setUp() {
        plotRepository = mock(PlotRepository.class);
        when(plotRepository.findListedPlots()).thenReturn(CompletableFuture.completedFuture(List.of()));

        plotManager = new PlotManager(plotRepository);
        skyblockHook = mock(SkyblockHook.class);

        YamlConfiguration config = new YamlConfiguration();
        config.set("limits.default-plots-per-island", 3);
        configManager = new ConfigManager(config);

        validator = new PlotValidator(plotManager, skyblockHook, configManager);
    }

    @Test
    @DisplayName("Devrait détecter le chevauchement avec une parcelle existante en cache")
    void shouldDetectOverlapWithExistingPlot() {
        RentPlot p1 = new RentPlot(
                1, UUID.randomUUID(), "island-1", "Champ-01", PlotType.FARM,
                "world", 0, 60, 0, 10, 70, 10,
                100, 3600, Set.of(), true, 0, System.currentTimeMillis()
        );
        plotManager.addPlot(p1);

        Cuboid overlapping = new Cuboid("world", 5, 60, 5, 15, 70, 15);
        Cuboid disjoint = new Cuboid("world", 20, 60, 0, 30, 70, 10);

        assertThat(plotManager.isOverlapping(overlapping)).isTrue();
        assertThat(plotManager.isOverlapping(disjoint)).isFalse();
    }

    @Test
    @DisplayName("Devrait refuser la création si le joueur ne possède pas d'île")
    void shouldRejectWhenPlayerHasNoIsland() {
        Player player = mock(Player.class);
        when(skyblockHook.hasIsland(player)).thenReturn(false);

        Cuboid cuboid = new Cuboid("world", 0, 60, 0, 10, 70, 10);
        ValidationResult result = validator.validate(player, "Champ-Test", cuboid);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getMessageKey()).isEqualTo("plots.error-no-island");
    }

    @Test
    @DisplayName("Devrait refuser si le joueur n'est pas le leader ou admin de l'île")
    void shouldRejectWhenPlayerIsNotIslandAdmin() {
        Player player = mock(Player.class);
        when(skyblockHook.hasIsland(player)).thenReturn(true);
        when(skyblockHook.getIslandId(player)).thenReturn("island-1");
        when(skyblockHook.isIslandAdminOrOwner(player, "island-1")).thenReturn(false);

        Cuboid cuboid = new Cuboid("world", 0, 60, 0, 10, 70, 10);
        ValidationResult result = validator.validate(player, "Champ-Test", cuboid);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getMessageKey()).isEqualTo("plots.error-not-owner");
    }

    @Test
    @DisplayName("Devrait refuser si les 8 sommets du cuboid ne sont pas intégralement dans l'île")
    void shouldRejectWhenCuboidExceedsIslandBoundary() {
        Player player = mock(Player.class);
        when(skyblockHook.hasIsland(player)).thenReturn(true);
        when(skyblockHook.getIslandId(player)).thenReturn("island-1");
        when(skyblockHook.isIslandAdminOrOwner(player, "island-1")).thenReturn(true);
        when(skyblockHook.isCuboidFullyInsideIsland(any(), anyString())).thenReturn(false);

        Cuboid cuboid = new Cuboid("world", 0, 60, 0, 10, 70, 10);
        ValidationResult result = validator.validate(player, "Champ-Test", cuboid);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getMessageKey()).isEqualTo("plots.error-outside-island");
    }

    @Test
    @DisplayName("Devrait refuser si le quota de parcelles par île est atteint")
    void shouldRejectWhenQuotaReached() {
        Player player = mock(Player.class);
        when(skyblockHook.hasIsland(player)).thenReturn(true);
        when(skyblockHook.getIslandId(player)).thenReturn("island-1");
        when(skyblockHook.isIslandAdminOrOwner(player, "island-1")).thenReturn(true);
        when(skyblockHook.isCuboidFullyInsideIsland(any(), anyString())).thenReturn(true);

        // Ajouter 3 parcelles (limite par défaut = 3)
        UUID owner = UUID.randomUUID();
        plotManager.addPlot(new RentPlot(1, owner, "island-1", "P1", PlotType.FARM, "world", 0, 0, 0, 5, 5, 5, 10, 3600, Set.of(), true, 0, 0));
        plotManager.addPlot(new RentPlot(2, owner, "island-1", "P2", PlotType.FARM, "world", 10, 0, 0, 15, 5, 5, 10, 3600, Set.of(), true, 0, 0));
        plotManager.addPlot(new RentPlot(3, owner, "island-1", "P3", PlotType.FARM, "world", 20, 0, 0, 25, 5, 5, 10, 3600, Set.of(), true, 0, 0));

        Cuboid cuboid = new Cuboid("world", 30, 0, 0, 35, 5, 5);
        ValidationResult result = validator.validate(player, "P4", cuboid);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getMessageKey()).isEqualTo("plots.error-limit-reached");
    }

    @Test
    @DisplayName("Devrait calculer la limite maximale autorisée selon les permissions LuckPerms")
    void shouldResolveMaxPlotsAllowedFromPermissions() {
        Player p1 = mock(Player.class);
        when(p1.getEffectivePermissions()).thenReturn(Set.of());
        assertThat(validator.getMaxPlotsAllowed(p1)).isEqualTo(3);

        Player p2 = mock(Player.class);
        Set<PermissionAttachmentInfo> perms = new HashSet<>();
        perms.add(new PermissionAttachmentInfo(p2, "danarent.limit.2", null, true));
        perms.add(new PermissionAttachmentInfo(p2, "danarent.limit.7", null, true));
        when(p2.getEffectivePermissions()).thenReturn(perms);
        assertThat(validator.getMaxPlotsAllowed(p2)).isEqualTo(7);

        Player pAdmin = mock(Player.class);
        when(pAdmin.hasPermission("danarent.admin")).thenReturn(true);
        assertThat(validator.getMaxPlotsAllowed(pAdmin)).isEqualTo(Integer.MAX_VALUE);
    }
}
