package net.danakube.danarent.util;

import org.bukkit.Material;
import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class BlockScannerTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Devrait scanner et extraire uniquement les matériaux distincts non-air et non-indestructibles")
    void shouldScanAndExtractMaterials() {
        World world = server.addSimpleWorld("test_world");
        Cuboid cuboid = new Cuboid("test_world", 0, 60, 0, 2, 60, 2);

        // Remplir certains blocs
        world.getBlockAt(0, 60, 0).setType(Material.STONE);
        world.getBlockAt(1, 60, 0).setType(Material.IRON_ORE);
        world.getBlockAt(2, 60, 0).setType(Material.DIAMOND_ORE);
        world.getBlockAt(0, 60, 1).setType(Material.AIR);
        world.getBlockAt(1, 60, 1).setType(Material.BEDROCK);
        world.getBlockAt(2, 60, 1).setType(Material.BARRIER);
        world.getBlockAt(0, 60, 2).setType(Material.STONE); // Doublon

        Set<Material> scanned = BlockScanner.scanPlotMaterials(cuboid);

        assertThat(scanned).containsExactlyInAnyOrder(Material.STONE, Material.IRON_ORE, Material.DIAMOND_ORE);
        assertThat(scanned).doesNotContain(Material.AIR, Material.BEDROCK, Material.BARRIER);
    }
}
