package net.danakube.danarent.api;

import net.danakube.danarent.config.ConfigManager;
import net.danakube.danarent.config.MessageManager;
import net.danakube.danarent.database.ContractRepository;
import net.danakube.danarent.database.PlotRepository;
import net.danakube.danarent.economy.EscrowService;

public interface DanaRentAPI {

    ConfigManager getConfigManager();

    MessageManager getMessageManager();

    PlotRepository getPlotRepository();

    ContractRepository getContractRepository();

    EscrowService getEscrowService();

    net.danakube.danarent.wand.SelectionManager getSelectionManager();

    net.danakube.danarent.plot.PlotManager getPlotManager();

    net.danakube.danarent.plot.PlotValidator getPlotValidator();

    net.danakube.danarent.hook.SkyblockHook getSkyblockHook();

    net.danakube.danarent.visual.ParticleVisualizer getParticleVisualizer();
}
