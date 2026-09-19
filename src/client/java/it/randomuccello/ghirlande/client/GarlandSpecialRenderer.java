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
 * Flat modular head renderer.
 *
 * The band and flower heads are deliberately separate. Eight slender green
 * segments form one continuous wreath around the head; then the saved flower
 * heads are placed over that band. Every visual is a two-sided plane with no
 * side faces, so the crown reads as pixel-art rather than as eight miniature
 * 3D item models.
 */
public final class GarlandSpecialRenderer implements SpecialModelRenderer<GarlandSpecialRenderer.RenderData> {
    private static final double BAND_RADIUS = 0.515D;
    private static final double FLOWER_RADIUS = 0.523D;
    private static final double BAND_Y = 0.505D;
    private static final double FLOWER_Y = 0.545D;

    // The transparent 16x16 head textures only occupy a small central area, so
    // this scale yields a visible blossom about one quarter of a head wide.
    private static final float FLOWER_SCALE = 0.88F;
    private static final float BAND_SCALE = 0.56F;

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

        // First render a single-looking green band from eight tangent 2D
        // segments. The segment size is chosen to slightly overlap neighbours,
        // avoiding gaps without creating the bulky stacked foliage of alpha.1.
        ItemStack band = GarlandVisuals.vineSegment();
        for (int i = 0; i < 8; i++) {
            submitPlane(band, ANGLES[i], BAND_RADIUS, BAND_Y, BAND_SCALE, 100 + i,
                    poseStack, collector, light, overlay, outlineColor);
        }

        int count = Math.min(8, flowers.size());
        for (int i = 0; i < count; i++) {
            ItemStack flower = GarlandVisuals.headFor(flowers.get(i));
            if (flower.isEmpty()) {
                continue;
            }
            submitPlane(flower, ANGLES[i], FLOWER_RADIUS, FLOWER_Y, FLOWER_SCALE, i,
                    poseStack, collector, light, overlay, outlineColor);
        }
    }

    private static void submitPlane(ItemStack stack, float degrees, double radius, double y,
                                    float scale, int seed,
                                    PoseStack poseStack, SubmitNodeCollector collector,
                                    int light, int overlay, int outlineColor) {
        double angle = Math.toRadians(degrees);
        double x = 0.5D + Math.cos(angle) * radius;
        double z = 0.5D + Math.sin(angle) * radius;

        poseStack.pushPose();
        poseStack.translate(x, y, z);
        // Plane faces radially outward; its local X axis stays tangent to the
        // wreath so the narrow green segments join the neighbours naturally.
        poseStack.rotateDegrees(Axis.YP, 90.0F - degrees);
        poseStack.scale(scale, scale, scale);

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
        output.accept(new Vector3f(-0.12F, 0.0F, -0.12F));
        output.accept(new Vector3f(1.12F, 1.05F, 1.12F));
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
