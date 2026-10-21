package net.danakube.danarent.economy;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.UUID;

public class EconomyHook {

    private Economy economy;

    public EconomyHook() {
        // Constructeur par défaut
    }

    public EconomyHook(Economy economy) {
        this.economy = economy;
    }

    public boolean setupEconomy() {
        if (Bukkit.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        this.economy = rsp.getProvider();
        return this.economy != null;
    }

    public boolean hasEconomy() {
        return economy != null;
    }

    public double getBalance(UUID playerUuid) {
        if (economy == null || playerUuid == null) return 0.0;
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUuid);
        return economy.getBalance(offlinePlayer);
    }

    public boolean hasEnough(UUID playerUuid, double amount) {
        if (economy == null || playerUuid == null) return false;
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUuid);
        return economy.has(offlinePlayer, amount);
    }

    public boolean withdraw(UUID playerUuid, double amount) {
        if (economy == null || playerUuid == null) return false;
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUuid);
        EconomyResponse response = economy.withdrawPlayer(offlinePlayer, amount);
        return response.transactionSuccess();
    }

    public boolean deposit(UUID playerUuid, double amount) {
        if (economy == null || playerUuid == null) return false;
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUuid);
        EconomyResponse response = economy.depositPlayer(offlinePlayer, amount);
        return response.transactionSuccess();
    }
}
