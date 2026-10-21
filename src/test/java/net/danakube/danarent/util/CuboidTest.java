package net.danakube.danarent.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CuboidTest {

    @Test
    @DisplayName("Devrait normaliser les coordonnées min et max quel que soit l'ordre des entrées")
    void shouldNormalizeCoordinates() {
        Cuboid cuboid = new Cuboid("world", 10, 50, -20, 0, 70, 10);

        assertThat(cuboid.getWorldName()).isEqualTo("world");
        assertThat(cuboid.getMinX()).isEqualTo(0);
        assertThat(cuboid.getMaxX()).isEqualTo(10);
        assertThat(cuboid.getMinY()).isEqualTo(50);
        assertThat(cuboid.getMaxY()).isEqualTo(70);
        assertThat(cuboid.getMinZ()).isEqualTo(-20);
        assertThat(cuboid.getMaxZ()).isEqualTo(10);
    }

    @Test
    @DisplayName("Devrait calculer le volume exact en blocs")
    void shouldCalculateVolumeAccurately() {
        // 0 to 2 = 3 blocks, 0 to 1 = 2 blocks, 0 to 4 = 5 blocks => 3 * 2 * 5 = 30
        Cuboid cuboid = new Cuboid("world", 0, 0, 0, 2, 1, 4);

        assertThat(cuboid.getVolume()).isEqualTo(30L);
    }

    @Test
    @DisplayName("Devrait vérifier l'inclusion d'un point à l'intérieur du cuboid")
    void shouldVerifyPointInclusion() {
        Cuboid cuboid = new Cuboid("world", 0, 0, 0, 10, 10, 10);

        assertThat(cuboid.contains("world", 5, 5, 5)).isTrue();
        assertThat(cuboid.contains("world", 0, 0, 0)).isTrue(); // Bordure min
        assertThat(cuboid.contains("world", 10, 10, 10)).isTrue(); // Bordure max
        assertThat(cuboid.contains("world", 11, 5, 5)).isFalse(); // Dehors X
        assertThat(cuboid.contains("other_world", 5, 5, 5)).isFalse(); // Autre monde
    }

    @Test
    @DisplayName("Devrait détecter correctement les chevauchements AABB")
    void shouldDetectAABBOverlaps() {
        Cuboid c1 = new Cuboid("world", 0, 0, 0, 10, 10, 10);
        Cuboid c2 = new Cuboid("world", 5, 5, 5, 15, 15, 15); // Chevauchement partiel
        Cuboid c3 = new Cuboid("world", 11, 0, 0, 20, 10, 10); // Disjoint en X
        Cuboid c4 = new Cuboid("other_world", 0, 0, 0, 10, 10, 10); // Monde différent
        Cuboid c5 = new Cuboid("world", 10, 10, 10, 20, 20, 20); // Contact sur un sommet

        assertThat(c1.overlaps(c2)).isTrue();
        assertThat(c2.overlaps(c1)).isTrue();
        assertThat(c1.overlaps(c3)).isFalse();
        assertThat(c1.overlaps(c4)).isFalse();
        assertThat(c1.overlaps(c5)).isTrue(); // Partage le bloc (10,10,10)
    }

    @Test
    @DisplayName("Devrait agrandir le cuboid avec expand(margin)")
    void shouldExpandCuboidCorrectly() {
        Cuboid cuboid = new Cuboid("world", 0, 10, 0, 10, 20, 10);
        Cuboid expanded = cuboid.expand(1.0);

        assertThat(expanded.getMinX()).isEqualTo(-1);
        assertThat(expanded.getMinY()).isEqualTo(9);
        assertThat(expanded.getMinZ()).isEqualTo(-1);
        assertThat(expanded.getMaxX()).isEqualTo(11);
        assertThat(expanded.getMaxY()).isEqualTo(21);
        assertThat(expanded.getMaxZ()).isEqualTo(11);
    }
}
