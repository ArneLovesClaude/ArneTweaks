package com.arnestyles;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArneStyles implements ModInitializer {
    public static final String MOD_ID = "arnestyles";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("ArneTweaks geladen.");
    }
}