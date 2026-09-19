package it.randomuccello.ghirlande.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import it.randomuccello.ghirlande.GarlandData;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Dynamic renderer for a garland. The eight ingredient flowers are read from the
 * ItemStack and rendered around a small ring, so mixed flowers remain visually
 * distinguishable without generating thousands of static textures/models.
 */
public final class GarlandSpecialRenderer implements SpecialModelRenderer<GarlandSpecialRenderer.RenderData> {
    private static final double RADIUS = 0.31D;
    private static final float FLOWER_SCALE = 0.24F;
    private static final float VINE_SCALE = 0.27F;

    @Override
    public RenderData extractArgument(ItemStack stack) {
        List<ItemStack> flowers = new ArrayList<>(8);
        GarlandData.fromStack(stack).ifPresent(data -> {
            for (String rawId : data.flowers()) {
                Identifier id = Identifier.tryParse(rawId);
                if (id == null) continue;
                Item item = BuiltInRegistries.ITEM.getValue(id);
                if (item != Items.AIR) {
                    flowers.add(new ItemStack(item));
                }
            }
        });
        return new RenderData(List.copyOf(flowers));
    }

    @Override
    public void submit(RenderData data, PoseStack poseStack, SubmitNodeCollector collector,
                       int light, int overlay, boolean foil, int outlineColor) {
        List<ItemStack> flowers = data.flowers();
        if (flowers.isEmpty()) {
            // A plain garland can still be visible in unusual/debug-created stacks.
            for (int i = 0; i < 8; i++) {
                submitPetal(Items.VINE.getDefaultInstance(), i, poseStack, collector, light, overlay, outlineColor, VINE_SCALE, true);
            }
            return;
        }

        for (int i = 0; i < 8; i++) {
            // A subtle green backing gives the flowers a wreath silhouette.
            submitPetal(Items.VINE.getDefaultInstance(), i, poseStack, collector, light, overlay, outlineColor, VINE_SCALE, true);
            ItemStack flower = flowers.get(i % flowers.size());
            submitPetal(flower, i, poseStack, collector, light, overlay, outlineColor, FLOWER_SCALE, false);
        }
    }

    private static void submitPetal(ItemStack stack, int index, PoseStack poseStack, SubmitNodeCollector collector,
                                    int light, int overlay, int outlineColor, float scale, boolean backing) {
        double angle = (Math.PI * 2.0D * index) / 8.0D;
        double x = 0.5D + Math.cos(angle) * RADIUS;
        double z = 0.5D + Math.sin(angle) * RADIUS;
        double y = backing ? 0.48D : 0.52D;

        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.rotateDegrees(Axis.YP, (float) Math.toDegrees(-angle) + 90.0F);
        poseStack.rotateDegrees(Axis.XP, -20.0F);
        poseStack.scale(scale, scale, scale);

        ItemStackRenderState renderState = new ItemStackRenderState();
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getItemModelResolver().updateForTopItem(
                renderState,
                stack,
                ItemDisplayContext.FIXED,
                minecraft.level,
                null,
                index
        );
        renderState.submit(poseStack, collector, light, overlay, outlineColor);
        poseStack.popPose();
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        output.accept(new Vector3f(0.0F, 0.0F, 0.0F));
        output.accept(new Vector3f(1.0F, 1.0F, 1.0F));
    }

    public record RenderData(List<ItemStack> flowers) {
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
