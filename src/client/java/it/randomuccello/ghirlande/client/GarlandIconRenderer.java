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
 * Flat dynamic inventory renderer.
 *
 * The first three saved flowers (top crafting row / forehead flowers) are
 * prominent. Rear ingredients are only hinted with faded copies over a simple
 * 16x16 green wreath. This keeps the icon flat while preserving the real recipe.
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

        if (flowers.size() >= 8) {
            submitFlower(GarlandVisuals.fadedModuleFor(flowers.get(5)), 0.36D, 0.38D, -10.0F, 0.47F, 5,
                    poseStack, collector, light, overlay, outlineColor);
            submitFlower(GarlandVisuals.fadedModuleFor(flowers.get(6)), 0.50D, 0.34D, 0.0F, 0.47F, 6,
                    poseStack, collector, light, overlay, outlineColor);
            submitFlower(GarlandVisuals.fadedModuleFor(flowers.get(7)), 0.64D, 0.38D, 10.0F, 0.47F, 7,
                    poseStack, collector, light, overlay, outlineColor);
        }

        submitFlower(GarlandVisuals.vineIcon(), 0.50D, 0.50D, 0.0F, 1.48F, 20,
                poseStack, collector, light, overlay, outlineColor);

        if (!flowers.isEmpty()) {
            int visible = Math.min(3, flowers.size());
            double[] xs = {0.31D, 0.50D, 0.69D};
            double[] ys = {0.59D, 0.62D, 0.59D};
            float[] rotations = {-9.0F, 0.0F, 9.0F};

            for (int i = 0; i < visible; i++) {
                submitFlower(GarlandVisuals.moduleFor(flowers.get(i)),
                        xs[i], ys[i], rotations[i], 0.68F, 30 + i,
                        poseStack, collector, light, overlay, outlineColor);
            }
        }
    }

    private static void submitFlower(ItemStack stack, double x, double y, float zRotation, float scale, int seed,
                                     PoseStack poseStack, SubmitNodeCollector collector,
                                     int light, int overlay, int outlineColor) {
        if (stack.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(x, y, 0.52D);
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
