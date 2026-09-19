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
 * Flat dynamic inventory icon.
 *
 * All layers are real two-sided planes. Rear flowers sit on a deeper Z layer,
 * the vine ring is in the middle, and the three front crafting-row flowers sit
 * in front. The deterministic depth separation removes the z-fighting present
 * in alpha.1 while preserving the approved 2D icon composition.
 */
public final class GarlandIconRenderer implements SpecialModelRenderer<GarlandIconRenderer.RenderData> {
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

        // Rear flowers: deliberately faint, small and on the deepest plane.
        if (flowers.size() >= 8) {
            submitPlane(GarlandVisuals.fadedHeadFor(flowers.get(5)), 0.37D, 0.39D, 0.44D,
                    -10.0F, 0.72F, 5, poseStack, collector, light, overlay, outlineColor);
            submitPlane(GarlandVisuals.fadedHeadFor(flowers.get(6)), 0.50D, 0.35D, 0.44D,
                    0.0F, 0.72F, 6, poseStack, collector, light, overlay, outlineColor);
            submitPlane(GarlandVisuals.fadedHeadFor(flowers.get(7)), 0.63D, 0.39D, 0.44D,
                    10.0F, 0.72F, 7, poseStack, collector, light, overlay, outlineColor);
        }

        // One flat wreath silhouette in the middle layer.
        submitPlane(GarlandVisuals.vineIcon(), 0.50D, 0.50D, 0.50D,
                0.0F, 0.90F, 20, poseStack, collector, light, overlay, outlineColor);

        // Only the three top-row/forehead ingredients are prominent, matching
        // the approved inventory mock-up.
        int visible = Math.min(3, flowers.size());
        double[] xs = {0.32D, 0.50D, 0.68D};
        double[] ys = {0.58D, 0.61D, 0.58D};
        float[] rotations = {-8.0F, 0.0F, 8.0F};

        for (int i = 0; i < visible; i++) {
            submitPlane(GarlandVisuals.headFor(flowers.get(i)),
                    xs[i], ys[i], 0.57D, rotations[i], 0.92F, 30 + i,
                    poseStack, collector, light, overlay, outlineColor);
        }
    }

    private static void submitPlane(ItemStack stack, double x, double y, double z,
                                    float zRotation, float scale, int seed,
                                    PoseStack poseStack, SubmitNodeCollector collector,
                                    int light, int overlay, int outlineColor) {
        if (stack.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.rotateDegrees(Axis.ZP, zRotation);
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
        output.accept(new Vector3f(0.0F, 0.0F, 0.0F));
        output.accept(new Vector3f(1.0F, 1.0F, 1.0F));
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
        public GarlandIconRenderer bake(SpecialModelRenderer.BakingContext context) {
            return new GarlandIconRenderer();
        }
    }
}
