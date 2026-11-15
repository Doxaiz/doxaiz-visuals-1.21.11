package com.lowcode.svintusclient.client.mixin;

import com.lowcode.svintusclient.client.module.ModuleManager;
import com.lowcode.svintusclient.client.module.impl.render.CustomSwordsModule;
import com.lowcode.svintusclient.client.module.impl.render.ItemModelsModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.HeldItemContext;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;

@Environment(EnvType.CLIENT)
@Mixin(ItemModelManager.class)
public abstract class ItemModelResolverMixin {

    @ModifyVariable(
        method = "update(Lnet/minecraft/client/render/item/ItemRenderState;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/world/World;Lnet/minecraft/util/HeldItemContext;I)V",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0,
        require = 1
    )
    private ItemStack svintus$replaceCustomItemModels(
        ItemStack stack,
        ItemRenderState renderState,
        ItemStack originalStack,
        ItemDisplayContext displayContext,
        World world,
        HeldItemContext itemOwner,
        int seed
    ) {
        if (stack == null || stack.isEmpty()) return stack;

        // If the item already has explicit custom model data strings (e.g. catalog cards / preview stacks), keep its own model
        CustomModelDataComponent existingCmd = stack.get(DataComponentTypes.CUSTOM_MODEL_DATA);
        if (existingCmd != null && !existingCmd.strings().isEmpty()) {
            return stack;
        }

        // 1. Custom Swords
        CustomSwordsModule swords = ModuleManager.get(CustomSwordsModule.class);
        if (swords != null && swords.isEnabled() && (stack.isIn(ItemTags.SWORDS) || isSword(stack))) {
            if (swords.selfOnly.get()) {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (itemOwner != null && itemOwner.getEntity() != null && itemOwner.getEntity() != mc.player) {
                    return stack;
                }
            }
            String selected = swords.getSelectedWeapon();
            if (selected != null && !selected.isBlank() && !selected.equalsIgnoreCase("Стандарт") && !selected.equalsIgnoreCase("Ванильные")) {
                ItemStack renderedStack = stack.copy();
                renderedStack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(List.of(), List.of(), List.of(selected), List.of()));
                return renderedStack;
            }
        }

        // 2. Custom Totems via ItemModelsModule
        ItemModelsModule itemModels = ModuleManager.get(ItemModelsModule.class);
        if (itemModels != null && itemModels.isEnabled() && stack.isOf(Items.TOTEM_OF_UNDYING)) {
            String totem = itemModels.getSelectedTotem();
            if (totem != null && !totem.isBlank() && !totem.equalsIgnoreCase("Ванильный")) {
                ItemStack renderedStack = stack.copy();
                renderedStack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(List.of(), List.of(), List.of(totem), List.of()));
                int glint = itemModels.getGlintColor();
                if (glint != -1) {
                    renderedStack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
                }
                return renderedStack;
            }
        }

        return stack;
    }

    private static boolean isSword(ItemStack stack) {
        return stack.isOf(Items.WOODEN_SWORD) || stack.isOf(Items.STONE_SWORD) ||
               stack.isOf(Items.IRON_SWORD) || stack.isOf(Items.GOLDEN_SWORD) ||
               stack.isOf(Items.DIAMOND_SWORD) || stack.isOf(Items.NETHERITE_SWORD);
    }
}
