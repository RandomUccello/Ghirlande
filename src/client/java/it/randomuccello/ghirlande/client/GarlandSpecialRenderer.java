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
 * Pixel-grid flower crown renderer.
 *
 * The crown is treated as an unwrapped 32x8 perimeter: front, right, back and
 * left are four exact 8-pixel faces. The four corner flowers are shared by the
 * two adjacent faces, so the eight saved recipe flowers remain the source of
 * truth while the rendered wreath stays visually continuous around corners.
 */
public final class GarlandSpecialRenderer implements SpecialModelRenderer<GarlandSpecialRenderer.RenderData> {
    // Alpha 6 already established the correct vertical fit. Keep that baseline.
    private static final double BAND_Y = 0.640D;
    private static final double FLOWER_Y = 0.655D;

    // Almost flush to the player head: no wide floating side ring.
    private static final double FACE_OFFSET = 0.0025D;
    private static final double FLOWER_OFFSET = 0.0040D;

    // The band slightly overlaps adjacent faces so the four 8px sections meet.
    private static final float BAND_SCALE_X = 1.035F;
    private static final float BAND_SCALE_Y = 0.285F;
    private static final float FLOWER_SCALE = 0.420F;

    /**
     * Three visible flowers per face: left shared corner, local centre, right
     * shared corner. Corner entries intentionally repeat the same recipe index
     * on two adjacent faces; they are not extra flowers.
     *
     * Small Y offsets reproduce the approved 32x8 strip's controlled 0-2 pixel
     * irregularity instead of placing all flowers on a ruler-straight row.
     */
    private static final FlowerPlane[] FLOWER_PLANES = {
            // Front: indices 0 - 1 - 2
            new FlowerPlane(0, 0.070D, -FLOWER_OFFSET, 180.0F, -0.008D),
            new FlowerPlane(1, 0.500D, -FLOWER_OFFSET, 180.0F,  0.004D),
            new FlowerPlane(2, 0.930D, -FLOWER_OFFSET, 180.0F, -0.002D),

            // Right: indices 2 - 4 - 7
            new FlowerPlane(2, 1.0D + FLOWER_OFFSET, 0.070D, 90.0F, -0.002D),
            new FlowerPlane(4, 1.0D + FLOWER_OFFSET, 0.500D, 90.0F, -0.010D),
            new FlowerPlane(7, 1.0D + FLOWER_OFFSET, 0.930D, 90.0F,  0.003D),

            // Back: indices 7 - 6 - 5
            new FlowerPlane(7, 0.930D, 1.0D + FLOWER_OFFSET, 0.0F,  0.003D),
            new FlowerPlane(6, 0.500D, 1.0D + FLOWER_OFFSET, 0.0F, -0.004D),
            new FlowerPlane(5, 0.070D, 1.0D + FLOWER_OFFSET, 0.0F,  0.006D),

            // Left: indices 5 - 3 - 0
            new FlowerPlane(5, -FLOWER_OFFSET, 0.930D, -90.0F,  0.006D),
            new FlowerPlane(3, -FLOWER_OFFSET, 0.500D, -90.0F,  0.000D),
            new FlowerPlane(0, -FLOWER_OFFSET, 0.070D, -90.0F, -0.008D)
    };

    private static final FaceBand[] FACE_BANDS = {
            new FaceBand(0.50D, -FACE_OFFSET, 180.0F),
            new FaceBand(1.0D + FACE_OFFSET, 0.50D, 90.0F),
            new FaceBand(0.50D, 1.0D + FACE_OFFSET, 0.0F),
            new FaceBand(-FACE_OFFSET, 0.50D, -90.0F)
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

        // One continuous-looking vine strip across the four exact head faces.
        ItemStack band = GarlandVisuals.vineFace();
        for (int i = 0; i < FACE_BANDS.length; i++) {
            FaceBand face = FACE_BANDS[i];
            submitPlane(band, face.x(), BAND_Y, face.z(), face.yaw(),
                    BAND_SCALE_X, BAND_SCALE_Y, 100 + i,
                    poseStack, collector, light, overlay, outlineColor);
        }

        int count = Math.min(8, flowers.size());
        for (int i = 0; i < FLOWER_PLANES.length; i++) {
            FlowerPlane plane = FLOWER_PLANES[i];
            if (plane.flowerIndex() >= count) {
                continue;
            }

            ItemStack flower = GarlandVisuals.headFor(flowers.get(plane.flowerIndex()));
            if (flower.isEmpty()) {
                continue;
            }

            submitPlane(flower, plane.x(), FLOWER_Y + plane.yOffset(), plane.z(), plane.yaw(),
                    FLOWER_SCALE, FLOWER_SCALE, 200 + i,
                    poseStack, collector, light, overlay, outlineColor);
        }
    }

    private static void submitPlane(ItemStack stack,
                                    double x, double y, double z, float yaw,
                                    float scaleX, float scaleY, int seed,
                                    PoseStack poseStack, SubmitNodeCollector collector,
                                    int light, int overlay, int outlineColor) {
        if (stack.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.rotateDegrees(Axis.YP, yaw);
        poseStack.scale(scaleX, scaleY, 1.0F);

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
        output.accept(new Vector3f(-0.05F, 0.0F, -0.05F));
        output.accept(new Vector3f(1.05F, 1.02F, 1.05F));
    }

    private record FlowerPlane(int flowerIndex, double x, double z, float yaw, double yOffset) {
    }

    private record FaceBand(double x, double z, float yaw) {
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
