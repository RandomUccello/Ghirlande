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
 * The eight saved crafting ingredients map one-to-one around the crown in ring
 * order: top-left, top, top-right, right, back-right, back, back-left, left.
 * Corner flowers are angled at 45 degrees so the wreath reads continuously from
 * front, side and three-quarter views without duplicating recipe ingredients.
 */
public final class GarlandSpecialRenderer implements SpecialModelRenderer<GarlandSpecialRenderer.RenderData> {
    private static final double BAND_Y = 0.765D;
    private static final double FLOWER_Y = 0.790D;
    private static final double FACE_OFFSET = 0.034D;
    private static final double FLOWER_OFFSET = 0.044D;
    private static final float BAND_SCALE_X = 1.06F;
    private static final float BAND_SCALE_Y = 0.19F;
    private static final float FLOWER_SCALE = 0.40F;

    private static final FlowerPlane[] FLOWER_PLANES = {
            new FlowerPlane(0, -0.010D, -0.010D, -135.0F),
            new FlowerPlane(1, 0.500D, -FLOWER_OFFSET, 180.0F),
            new FlowerPlane(2, 1.010D, -0.010D, 135.0F),
            new FlowerPlane(4, 1.0D + FLOWER_OFFSET, 0.500D, 90.0F),
            new FlowerPlane(7, 1.010D, 1.010D, 45.0F),
            new FlowerPlane(6, 0.500D, 1.0D + FLOWER_OFFSET, 0.0F),
            new FlowerPlane(5, -0.010D, 1.010D, -45.0F),
            new FlowerPlane(3, -FLOWER_OFFSET, 0.500D, -90.0F)
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
        output.accept(new Vector3f(-0.14F, 0.0F, -0.14F));
        output.accept(new Vector3f(1.14F, 1.08F, 1.14F));
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
