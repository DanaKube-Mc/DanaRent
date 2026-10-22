package net.danakube.danarent.util;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LimitResolverTest {

    @Test
    @DisplayName("Devrait retourner la valeur par défaut si aucune permission n'est définie")
    void shouldReturnDefaultValues() {
        Plugin plugin = mock(Plugin.class);
        YamlConfiguration config = new YamlConfiguration();
        config.set("limits.creation.default", 3);
        config.set("limits.active-rentals.default", 1);
        when(plugin.getConfig()).thenReturn(config);

        Player player = mock(Player.class);
        when(player.getEffectivePermissions()).thenReturn(Set.of());

        assertThat(LimitResolver.getMaxCreations(plugin, player)).isEqualTo(3);
        assertThat(LimitResolver.getMaxActiveRentals(plugin, player)).isEqualTo(1);
    }

    @Test
    @DisplayName("Devrait retourner MAX_VALUE pour les admins ou permissions unlimited")
    void shouldReturnMaxValueForAdmins() {
        Plugin plugin = mock(Plugin.class);
        Player admin = mock(Player.class);
        when(admin.hasPermission("danarent.admin")).thenReturn(true);

        assertThat(LimitResolver.getMaxCreations(plugin, admin)).isEqualTo(Integer.MAX_VALUE);
        assertThat(LimitResolver.getMaxActiveRentals(plugin, admin)).isEqualTo(Integer.MAX_VALUE);

        Player unlimitedRent = mock(Player.class);
        when(unlimitedRent.hasPermission("danarent.limit.rent.unlimited")).thenReturn(true);
        assertThat(LimitResolver.getMaxActiveRentals(plugin, unlimitedRent)).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    @DisplayName("Devrait résoudre la valeur numérique la plus élevée pour les créations et locations")
    void shouldResolveHighestPermissionValue() {
        Plugin plugin = mock(Plugin.class);
        YamlConfiguration config = new YamlConfiguration();
        config.set("limits.creation.default", 3);
        config.set("limits.active-rentals.default", 1);
        when(plugin.getConfig()).thenReturn(config);

        Player player = mock(Player.class);
        Set<PermissionAttachmentInfo> perms = new HashSet<>();
        perms.add(new PermissionAttachmentInfo(player, "danarent.limit.create.5", null, true));
        perms.add(new PermissionAttachmentInfo(player, "danarent.limit.create.10", null, true));
        perms.add(new PermissionAttachmentInfo(player, "danarent.limit.create.2", null, true));

        perms.add(new PermissionAttachmentInfo(player, "danarent.limit.rent.2", null, true));
        perms.add(new PermissionAttachmentInfo(player, "danarent.limit.rent.4", null, true));

        when(player.getEffectivePermissions()).thenReturn(perms);

        assertThat(LimitResolver.getMaxCreations(plugin, player)).isEqualTo(10);
        assertThat(LimitResolver.getMaxActiveRentals(plugin, player)).isEqualTo(4);
    }
}
