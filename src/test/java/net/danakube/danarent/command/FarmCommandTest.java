package net.danakube.danarent.command;

import net.danakube.danarent.DanaRentPlugin;
import net.danakube.danarent.hook.SkyblockHook;
import net.danakube.danarent.model.PlotType;
import net.danakube.danarent.model.RentPlot;
import net.danakube.danarent.wand.PlayerSelection;
import net.danakube.danarent.wand.WandFactory;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FarmCommandTest {

    private ServerMock server;
    private DanaRentPlugin plugin;
    private SkyblockHook skyblockHook;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(DanaRentPlugin.class);

        skyblockHook = mock(SkyblockHook.class);
        when(skyblockHook.hasIsland(any(Player.class))).thenReturn(true);
        when(skyblockHook.getIslandId(any(Player.class))).thenReturn("test-island-1");
        when(skyblockHook.isIslandAdminOrOwner(any(Player.class), anyString())).thenReturn(true);
        when(skyblockHook.isCuboidFullyInsideIsland(any(), anyString())).thenReturn(true);

        plugin.setSkyblockHook(skyblockHook);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Devrait donner la baguette de sélection avec /farm wand")
    void shouldGiveWandOnCommand() {
        PlayerMock player = server.addPlayer();
        player.addAttachment(plugin, "danarent.use", true);
        player.addAttachment(plugin, "danarent.owner", true);

        boolean handled = player.performCommand("farm wand");
        assertThat(handled).isTrue();

        boolean foundWand = false;
        for (ItemStack item : player.getInventory().getContents()) {
            if (WandFactory.isWand(item, plugin)) {
                foundWand = true;
                break;
            }
        }
        assertThat(foundWand).isTrue();
    }

    @Test
    @DisplayName("Devrait refuser /farm create sans sélection préalable")
    void shouldRejectCreateWithoutSelection() {
        PlayerMock player = server.addPlayer();
        player.addAttachment(plugin, "danarent.use", true);
        player.addAttachment(plugin, "danarent.owner", true);

        player.performCommand("farm create MonChamp");

        String nextMsg = player.nextMessage();
        assertThat(nextMsg).contains("définir les deux coins");
    }

    @Test
    @DisplayName("Devrait valider et créer la parcelle avec /farm create lorsque la sélection est complète")
    void shouldCreatePlotWhenSelectionIsValid() throws InterruptedException {
        PlayerMock player = server.addPlayer();
        player.addAttachment(plugin, "danarent.use", true);
        player.addAttachment(plugin, "danarent.owner", true);

        World world = player.getWorld();
        PlayerSelection sel = plugin.getSelectionManager().getOrCreateSelection(player.getUniqueId());
        sel.setPos1(new Location(world, 0, 60, 0));
        sel.setPos2(new Location(world, 10, 70, 10));

        player.performCommand("farm create ChampBlé farm");

        // Attente de l'enregistrement asynchrone
        for (int i = 0; i < 20; i++) {
            if (plugin.getPlotManager().getPlotByName("ChampBlé", player.getUniqueId()).isPresent()) {
                break;
            }
            Thread.sleep(50);
        }

        String msg = player.nextMessage();
        assertThat(msg).contains("ChampBlé");

        assertThat(plugin.getPlotManager().getPlotByName("ChampBlé", player.getUniqueId())).isPresent();
    }

    @Test
    @DisplayName("Devrait supprimer une parcelle avec /farm delete si aucun contrat actif")
    void shouldDeletePlotWithCommand() throws InterruptedException {
        PlayerMock player = server.addPlayer();
        player.addAttachment(plugin, "danarent.use", true);
        player.addAttachment(plugin, "danarent.owner", true);

        World world = player.getWorld();
        PlayerSelection sel = plugin.getSelectionManager().getOrCreateSelection(player.getUniqueId());
        sel.setPos1(new Location(world, 0, 60, 0));
        sel.setPos2(new Location(world, 10, 70, 10));
        player.performCommand("farm create A_Supprimer");

        for (int i = 0; i < 20; i++) {
            if (plugin.getPlotManager().getPlotByName("A_Supprimer", player.getUniqueId()).isPresent()) {
                break;
            }
            Thread.sleep(50);
        }
        player.nextMessage(); // Consommer le message de création

        player.performCommand("farm delete A_Supprimer");

        for (int i = 0; i < 20; i++) {
            if (plugin.getPlotManager().getPlotByName("A_Supprimer", player.getUniqueId()).isEmpty()) {
                break;
            }
            Thread.sleep(50);
        }

        String delMsg = player.nextMessage();
        assertThat(delMsg).contains("supprimé");

        assertThat(plugin.getPlotManager().getPlotByName("A_Supprimer", player.getUniqueId())).isEmpty();
    }

    @Test
    @DisplayName("Devrait proposer les sous-commandes via l'auto-complétion (TabCompleter)")
    void shouldProvideTabCompletions() {
        PlayerMock player = server.addPlayer();
        player.addAttachment(plugin, "danarent.owner", true);

        List<String> completions = server.getCommandTabComplete(player, "farm ");
        assertThat(completions).contains("wand", "create", "delete");
    }
}
