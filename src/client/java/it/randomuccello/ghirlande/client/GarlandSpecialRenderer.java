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
 * Flat flower-crown renderer following the square perimeter of a Minecraft head.
 *
 * The eight saved crafting ingredients stay the source of truth. Corner flowers
 * are submitted once on each adjacent face, giving twelve visible planes while
 * still representing exactly eight logical flowers. This makes the wreath read
 * as a continuous crown from front, side and back without inventing ingredients.
 */
public final class GarlandSpecialRenderer implements SpecialModelRenderer<GarlandSpecialRenderer.RenderData> {
    // Raised to the upper forehead/hairline instead of crossing the eyes.
    private static final double BAND_Y = 0.865D;
    private static final double FLOWER_Y = 0.885D;

    // Keep the wreath just outside the head skin, with a tiny flower offset to
    // prevent coplanar flicker against the greenery.
    private static final double FACE_OFFSET = 0.040D;
    private static final double FLOWER_OFFSET = 0.052D;

    // Thin continuous greenery, intentionally much less bulky than alpha.2.
    private static final float BAND_SCALE_X = 1.10F;
    private static final float BAND_SCALE_Y = 0.30F;

    // Final simple-flower standard: blossoms are small accents on the wreath,
    // approximately 20-25% of a head width rather than face-sized sprites.
    private static final float FLOWER_SCALE = 0.235F;

    /*
     * Visible planes. The integer selects one of the eight recipe flowers:
     *
     * recipe: 0 1 2 / 3 _ 4 / 5 6 7
     *
     * front: 0 1 2
     * left : 0 3 5   (corner flowers 0/5 are shared)
     * back : 5 6 7
     * right: 2 4 7   (corner flowers 2/7 are shared)
     *
     * This yields the visual density of the approved concept while preserving
     * the exact flower identities used in crafting.
     */
    private static final FlowerPlane[] FLOWER_PLANES = {
            // Front forehead, left to right.
            new FlowerPlane(0, 0.19D, -FLOWER_OFFSET, 180.0F),
            new FlowerPlane(1, 0.50D, -FLOWER_OFFSET, 180.0F),
            new FlowerPlane(2, 0.81D, -FLOWER_OFFSET, 180.0F),

            // Left temple / side, front to back.
            new FlowerPlane(0, -FLOWER_OFFSET, 0.19D, -90.0F),
            new FlowerPlane(3, -FLOWER_OFFSET, 0.50D, -90.0F),
            new FlowerPlane(5, -FLOWER_OFFSET, 0.81D, -90.0F),

            // Back, left to right when viewed from behind.
            new FlowerPlane(5, 0.19D, 1.0D + FLOWER_OFFSET, 0.0F),
            new FlowerPlane(6, 0.50D, 1.0D + FLOWER_OFFSET, 0.0F),
            new FlowerPlane(7, 0.81D, 1.0D + FLOWER_OFFSET, 0.0F),

            // Right temple / side, back to front in local Z.
            new FlowerPlane(7, 1.0D + FLOWER_OFFSET, 0.81D, 90.0F),
            new FlowerPlane(4, 1.0D + FLOWER_OFFSET, 0.50D, 90.0F),
            new FlowerPlane(2, 1.0D + FLOWER_OFFSET, 0.19D, 90.0F)
    };

    private static final FaceBand[] FACE_BANDS = {
            new FaceBand(0.50D, -FACE_OFFSET, 180.0F),
            new FaceBand(-FACE_OFFSET, 0.50D, -90.0F),
            new FaceBand(1.0D + FACE_OFFSET, 0.50D, 90.0F),
            new FaceBand(0.50D, 1.0D + FACE_OFFSET, 0.0F)
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

            submitPlane(flower, plane.x(), FLOWER_Y, plane.z(), plane.yaw(),
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
        output.accept(new Vector3f(-0.12F, 0.0F, -0.12F));
        output.accept(new Vector3f(1.12F, 1.10F, 1.12F));
    }

    private record FlowerPlane(int flowerIndex, double x, double z, float yaw) {
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
