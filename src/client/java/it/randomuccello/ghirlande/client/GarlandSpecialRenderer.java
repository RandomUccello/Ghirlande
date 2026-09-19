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
 * Flat square-perimeter garland renderer.
 *
 * Minecraft heads are square, so the wreath follows the four faces of the head
 * instead of placing ingredients on a mathematical circle. The eight saved
 * crafting ingredients keep their crafting-grid order:
 *
 * 0 1 2  -> front forehead
 * 3   4  -> left/right temples
 * 5 6 7  -> back of the head
 *
 * The greenery is rendered as four thin continuous face bands. Flower heads
 * are independent 2D planes placed on top, so the crown stays delicate and the
 * blossoms do not inherit bulky stems or overlapping connector geometry.
 */
public final class GarlandSpecialRenderer implements SpecialModelRenderer<GarlandSpecialRenderer.RenderData> {
    private static final double BAND_Y = 0.705D;
    private static final double FLOWER_Y = 0.730D;
    private static final double FACE_OFFSET = 0.055D;

    // Full-width thin band on each face. X is tangent to the face, Y vertical.
    private static final float BAND_SCALE_X = 1.18F;
    private static final float BAND_SCALE_Y = 0.55F;

    // Head sprites are intentionally compact inside their 16x16 canvases.
    // This scale makes each blossom roughly one fifth to one quarter of a head
    // wide, matching the approved concept instead of dominating the face.
    private static final float FLOWER_SCALE = 0.52F;

    // 0 1 2 / 3 _ 4 / 5 6 7. Coordinates follow the square head perimeter.
    private static final Slot[] FLOWER_SLOTS = {
            new Slot(0.18D, -FACE_OFFSET, 180.0F),
            new Slot(0.50D, -FACE_OFFSET, 180.0F),
            new Slot(0.82D, -FACE_OFFSET, 180.0F),
            new Slot(-FACE_OFFSET, 0.50D, -90.0F),
            new Slot(1.0D + FACE_OFFSET, 0.50D, 90.0F),
            new Slot(0.18D, 1.0D + FACE_OFFSET, 0.0F),
            new Slot(0.50D, 1.0D + FACE_OFFSET, 0.0F),
            new Slot(0.82D, 1.0D + FACE_OFFSET, 0.0F)
    };

    private static final FaceBand[] FACE_BANDS = {
            new FaceBand(0.50D, -FACE_OFFSET + 0.010D, 180.0F),
            new FaceBand(-FACE_OFFSET + 0.010D, 0.50D, -90.0F),
            new FaceBand(1.0D + FACE_OFFSET - 0.010D, 0.50D, 90.0F),
            new FaceBand(0.50D, 1.0D + FACE_OFFSET - 0.010D, 0.0F)
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
        for (int i = 0; i < count; i++) {
            ItemStack flower = GarlandVisuals.headFor(flowers.get(i));
            if (flower.isEmpty()) {
                continue;
            }

            Slot slot = FLOWER_SLOTS[i];
            submitPlane(flower, slot.x(), FLOWER_Y, slot.z(), slot.yaw(),
                    FLOWER_SCALE, FLOWER_SCALE, i,
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
        output.accept(new Vector3f(-0.18F, 0.0F, -0.18F));
        output.accept(new Vector3f(1.18F, 1.10F, 1.18F));
    }

    private record Slot(double x, double z, float yaw) {
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
