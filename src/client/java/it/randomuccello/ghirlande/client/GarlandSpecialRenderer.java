package it.randomuccello.ghirlande.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Head renderer for the modular garland.
 *
 * The eight saved crafting ingredients are mapped around the player's head in
 * the same spatial order as the outside cells of the crafting grid:
 * top row = forehead, middle left/right = temples, bottom row = back.
 *
 * Dandelion and poppy use the new 16x16 modular sprites. Other flowers keep the
 * old vanilla-item fallback during this prototype.
 */
public final class GarlandSpecialRenderer implements SpecialModelRenderer<GarlandSpecialRenderer.RenderData> {
    private static final double RADIUS = 0.54D;
    private static final double BAND_Y = 0.51D;
    private static final float MODULE_SCALE = 0.82F;

    // Angles mirror the eight occupied crafting-grid slots:
    // 0 1 2 / 3 _ 4 / 5 6 7. Front is the negative-Z side of the local head.
    private static final float[] ANGLES = {
            225.0F, 270.0F, 315.0F,
            180.0F, 0.0F,
            135.0F, 90.0F, 45.0F
    };

    @Override
    public RenderData extractArgument(ItemStack stack) {
        return new RenderData(List.copyOf(GarlandVisuals.flowerIds(stack)));
    }

    @Override
    public void submit(RenderData data, PoseStack poseStack, SubmitNodeCollector collector,
                       int light, int overlay, boolean foil, int outlineColor) {
        List<String> flowers = data.flowers();
        if (flowers.isEmpty()) {
            return;
        }

        int count = Math.min(8, flowers.size());
        for (int i = 0; i < count; i++) {
            ItemStack visual = GarlandVisuals.moduleFor(flowers.get(i));
            if (visual.isEmpty()) {
                continue;
            }
            submitModule(visual, ANGLES[i], i, poseStack, collector, light, overlay, outlineColor);
        }
    }

    private static void submitModule(ItemStack stack, float degrees, int seed,
                                     PoseStack poseStack, SubmitNodeCollector collector,
                                     int light, int overlay, int outlineColor) {
        double angle = Math.toRadians(degrees);
        double x = 0.5D + Math.cos(angle) * RADIUS;
        double z = 0.5D + Math.sin(angle) * RADIUS;

        poseStack.pushPose();
        poseStack.translate(x, BAND_Y, z);
        // Each flat module faces away from the centre of the head. Its left/right
        // connector axis is therefore tangent to the ring and joins neighbours.
        poseStack.rotateDegrees(Axis.YP, 90.0F - degrees);
        poseStack.scale(MODULE_SCALE, MODULE_SCALE, MODULE_SCALE);

        ItemStackRenderState renderState = new ItemStackRenderState();
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getItemModelResolver().updateForTopItem(
                renderState,
                stack,
                ItemDisplayContext.FIXED,
                minecraft.level,
                null,
                seed
        );
        renderState.submit(poseStack, collector, light, overlay, outlineColor);
        poseStack.popPose();
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        output.accept(new Vector3f(-0.2F, 0.0F, -0.2F));
        output.accept(new Vector3f(1.2F, 1.1F, 1.2F));
    }

    public record RenderData(List<String> flowers) {
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked<RenderData> {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public GarlandSpecialRenderer bake(SpecialModelRenderer.BakingContext context) {
            return new GarlandSpecialRenderer();
        }
    }
}
