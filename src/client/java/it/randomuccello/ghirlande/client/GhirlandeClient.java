package it.randomuccello.ghirlande.client;

import it.randomuccello.ghirlande.GhirlandeMod;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.special.SpecialModelRenderers;

public final class GhirlandeClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SpecialModelRenderers.ID_MAPPER.put(GhirlandeMod.id("garland"), GarlandSpecialRenderer.Unbaked.MAP_CODEC);
    }
}
