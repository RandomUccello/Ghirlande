package it.randomuccello.ghirlande;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GhirlandeMod implements ModInitializer {
    public static final String MOD_ID = "ghirlande";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Ghirlande initialized.");
    }
}
