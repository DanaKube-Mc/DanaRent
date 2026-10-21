package net.danakube.danarent.wand;

import net.danakube.danarent.DanaRentPlugin;
import net.danakube.danarent.util.Cuboid;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WandSelectionTest {

    private ServerMock server;
    private DanaRentPlugin plugin;
    private SelectionManager selectionManager;
    private WandListener wandListener;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(DanaRentPlugin.class);
        selectionManager = new SelectionManager();
        wandListener = new WandListener(plugin, selectionManager);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Devrait créer une baguette avec le tag PDC sécurisé et identifier les faux items")
    void shouldCreateWandWithPdcAndIdentifyFakes() {
        ItemStack wand = WandFactory.createWand(plugin);
        assertThat(wand).isNotNull();
        assertThat(WandFactory.isWand(wand, plugin)).isTrue();

        // Une houe normale sans le tag PDC ne doit PAS être reconnue
        ItemStack normalHoe = new ItemStack(Material.GOLDEN_HOE);
        assertThat(WandFactory.isWand(normalHoe, plugin)).isFalse();
        assertThat(WandFactory.isWand(null, plugin)).isFalse();
    }

    @Test
    @DisplayName("Devrait gérer la sélection des points et convertir en Cuboid")
    void shouldManageSelectionAndConvertToCuboid() {
        World world = server.addSimpleWorld("test_world");
        Location loc1 = new Location(world, 10, 64, 10);
        Location loc2 = new Location(world, 20, 70, 20);

        PlayerSelection selection = new PlayerSelection();
        assertThat(selection.hasBothPositions()).isFalse();

        selection.setPos1(loc1);
        assertThat(selection.hasBothPositions()).isFalse();

        selection.setPos2(loc2);
        assertThat(selection.hasBothPositions()).isTrue();

        Cuboid cuboid = selection.toCuboid();
        assertThat(cuboid).isNotNull();
        assertThat(cuboid.getVolume()).isEqualTo(11L * 7L * 11L);
    }

    @Test
    @DisplayName("Devrait interdire la création d'un cuboid si les deux points sont dans des mondes différents")
    void shouldThrowIfPointsInDifferentWorlds() {
        World w1 = server.addSimpleWorld("w1");
        World w2 = server.addSimpleWorld("w2");

        PlayerSelection selection = new PlayerSelection();
        selection.setPos1(new Location(w1, 0, 0, 0));
        selection.setPos2(new Location(w2, 10, 10, 10));

        assertThatThrownBy(selection::toCuboid)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("mondes différents");
    }

    @Test
    @DisplayName("Devrait intercepter le clic gauche et clic droit avec la wand pour définir pos1 et pos2 sans altérer le monde")
    void shouldHandleWandClicksAndCancelEvents() {
        PlayerMock player = server.addPlayer();
        ItemStack wand = WandFactory.createWand(plugin);
        player.getInventory().setItemInMainHand(wand);

        World world = player.getWorld();
        Block block1 = world.getBlockAt(5, 64, 5);
        Block block2 = world.getBlockAt(15, 64, 15);

        // Clic gauche (Pos 1)
        PlayerInteractEvent leftClick = new PlayerInteractEvent(player, Action.LEFT_CLICK_BLOCK, wand, block1, null, EquipmentSlot.HAND);
        wandListener.onInteract(leftClick);

        assertThat(leftClick.isCancelled()).isTrue();
        PlayerSelection sel = selectionManager.getSelection(player.getUniqueId());
        assertThat(sel).isNotNull();
        assertThat(sel.getPos1()).isEqualTo(block1.getLocation());

        // Clic droit (Pos 2)
        PlayerInteractEvent rightClick = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, wand, block2, null, EquipmentSlot.HAND);
        wandListener.onInteract(rightClick);

        assertThat(rightClick.isCancelled()).isTrue();
        assertThat(sel.getPos2()).isEqualTo(block2.getLocation());
        assertThat(sel.hasBothPositions()).isTrue();
    }

    @Test
    @DisplayName("Devrait nettoyer la sélection à la déconnexion pour éviter les fuites mémoire")
    void shouldClearSelectionOnQuit() {
        PlayerMock player = server.addPlayer();
        selectionManager.getOrCreateSelection(player.getUniqueId()).setPos1(player.getLocation());

        assertThat(selectionManager.getSelection(player.getUniqueId())).isNotNull();

        PlayerQuitEvent quitEvent = new PlayerQuitEvent(player, (net.kyori.adventure.text.Component) null);
        wandListener.onQuit(quitEvent);

        assertThat(selectionManager.getSelection(player.getUniqueId())).isNull();
    }
}
