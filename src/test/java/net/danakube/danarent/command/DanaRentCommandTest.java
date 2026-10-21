package net.danakube.danarent.command;

import net.danakube.danarent.DanaRentPlugin;
import net.danakube.danarent.model.PlotType;
import net.danakube.danarent.model.RentPlot;
import org.bukkit.command.ConsoleCommandSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DanaRentCommandTest {

    private ServerMock server;
    private DanaRentPlugin plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(DanaRentPlugin.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Devrait refuser /danarent reload sans la permission danarent.admin")
    void shouldDenyReloadWithoutPermission() {
        PlayerMock player = server.addPlayer();
        player.addAttachment(plugin, "danarent.use", true);

        player.performCommand("danarent reload");

        String msg = player.nextMessage();
        assertThat(msg).isNotNull().contains("permission");
    }

    @Test
    @DisplayName("Devrait exécuter /danarent reload avec la permission danarent.admin")
    void shouldAllowReloadWithAdminPermission() {
        PlayerMock player = server.addPlayer();
        player.addAttachment(plugin, "danarent.admin", true);

        player.performCommand("danarent reload");

        String msg = player.nextMessage();
        assertThat(msg).isNotNull().contains("rechargées avec succès");
    }

    @Test
    @DisplayName("Devrait exécuter /danarent reload depuis la console")
    void shouldAllowReloadFromConsole() {
        ConsoleCommandSender console = server.getConsoleSender();
        server.dispatchCommand(console, "danarent reload");
        // No exception thrown
    }

    @Test
    @DisplayName("Devrait refuser /danarent collect depuis la console")
    void shouldRejectCollectFromConsole() {
        ConsoleCommandSender console = server.getConsoleSender();
        server.dispatchCommand(console, "danarent collect");
        // No exception thrown
    }

    @Test
    @DisplayName("Devrait refuser /danarent collect sans la permission danarent.owner")
    void shouldDenyCollectWithoutPermission() {
        PlayerMock player = server.addPlayer();
        player.addAttachment(plugin, "danarent.use", true);

        player.performCommand("danarent collect");

        String msg = player.nextMessage();
        assertThat(msg).isNotNull().contains("permission");
    }

    @Test
    @DisplayName("Devrait afficher aucun revenu si la cagnotte est vide lors de /danarent collect")
    void shouldNotifyNoFundsWhenEmpty() throws InterruptedException {
        PlayerMock player = server.addPlayer();
        player.addAttachment(plugin, "danarent.owner", true);

        player.performCommand("danarent collect");

        String msg = null;
        for (int i = 0; i < 30; i++) {
            msg = player.nextMessage();
            if (msg != null) break;
            Thread.sleep(50);
        }

        assertThat(msg).isNotNull().contains("aucun revenu");
    }

    @Test
    @DisplayName("Devrait collecter les fonds disponibles lors de /danarent collect")
    void shouldCollectPendingFunds() throws InterruptedException {
        net.danakube.danarent.economy.EconomyHook mockHook = org.mockito.Mockito.mock(net.danakube.danarent.economy.EconomyHook.class);
        org.mockito.Mockito.when(mockHook.deposit(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyDouble())).thenReturn(true);
        plugin.setEconomyHook(mockHook);

        PlayerMock player = server.addPlayer();
        player.addAttachment(plugin, "danarent.owner", true);

        RentPlot plot = new RentPlot(
                0, player.getUniqueId(), "island-1", "FermeTest", PlotType.FARM,
                "world", 0, 60, 0, 10, 70, 10,
                100.0, 3600, Set.of(), true, 250.0, System.currentTimeMillis()
        );
        int plotId = plugin.getPlotRepository().save(plot).join();
        plot.setId(plotId);
        plugin.getPlotManager().addPlot(plot);

        player.performCommand("danarent collect");

        String msg = null;
        for (int i = 0; i < 30; i++) {
            msg = player.nextMessage();
            if (msg != null) break;
            Thread.sleep(50);
        }

        assertThat(msg).isNotNull().contains("250");
        assertThat(plot.getPendingRevenue()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Devrait refuser /danarent listall sans la permission danarent.admin")
    void shouldDenyListAllWithoutPermission() {
        PlayerMock player = server.addPlayer();
        player.addAttachment(plugin, "danarent.owner", true);

        player.performCommand("danarent listall");

        String msg = player.nextMessage();
        assertThat(msg).isNotNull().contains("permission");
    }

    @Test
    @DisplayName("Devrait lister toutes les parcelles du serveur avec /danarent listall")
    void shouldListAllPlotsWithAdmin() {
        PlayerMock admin = server.addPlayer();
        admin.addAttachment(plugin, "danarent.admin", true);

        RentPlot plot = new RentPlot(
                1, admin.getUniqueId(), "island-1", "MegaCarriere", PlotType.MINING,
                "world", 0, 60, 0, 10, 70, 10,
                150.0, 3600, Set.of(), true, 0.0, System.currentTimeMillis()
        );
        plugin.getPlotManager().addPlot(plot);

        admin.performCommand("danarent listall");

        String header = admin.nextMessage();
        assertThat(header).isNotNull();
        String plainHeader = org.bukkit.ChatColor.stripColor(header);
        assertThat(plainHeader).contains("Liste des Parcelles");

        String item = admin.nextMessage();
        assertThat(item).isNotNull().contains("MegaCarriere").contains("MINING");
    }

    @Test
    @DisplayName("Devrait adapter les auto-complétions de /danarent selon les permissions")
    void shouldProvideCompletionsBasedOnPermissions() {
        PlayerMock user = server.addPlayer();
        List<String> noPerms = server.getCommandTabComplete(user, "danarent ");
        assertThat(noPerms).isEmpty();

        PlayerMock owner = server.addPlayer();
        owner.addAttachment(plugin, "danarent.owner", true);
        List<String> ownerCompletions = server.getCommandTabComplete(owner, "danarent ");
        assertThat(ownerCompletions).containsExactly("collect");

        PlayerMock admin = server.addPlayer();
        admin.addAttachment(plugin, "danarent.admin", true);
        List<String> adminCompletions = server.getCommandTabComplete(admin, "danarent ");
        assertThat(adminCompletions).contains("reload", "listall", "collect");
    }
}
