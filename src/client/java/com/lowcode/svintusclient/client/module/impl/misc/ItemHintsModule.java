package com.lowcode.svintusclient.client.module.impl.misc;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Locale;

/**
 * ItemHintsModule — расширенные подсказки в тултипах для компасов, сфер и талисманов HolyWorld.
 * Адаптировано из ItemTooltipHandler (Doxaiz-visuals).
 */
public final class ItemHintsModule extends Module {

    public final BooleanSetting compassTooltip = add(new BooleanSetting("Подсказки компаса", true));
    public final BooleanSetting talismanInfo = add(new BooleanSetting("Свойства талисманов", true));
    public final BooleanSetting potionTiers = add(new BooleanSetting("Уровни и время зелий", true));
    public final BooleanSetting repairCost = add(new BooleanSetting("Стоимость починки", true));
    public final BooleanSetting enchantmentMax = add(new BooleanSetting("Максимальный уровень чар", true));

    public ItemHintsModule() {
        super("Подсказки предметов", "Расширенные всплывающие подсказки для компасов, сфер и талисманов", Category.UTILITIES);
    }

    public void handleTooltip(ItemStack stack, List<Text> lines) {
        if (!isEnabled() || stack == null || stack.isEmpty()) return;

        String name = stack.getName().getString().toLowerCase(Locale.ROOT);

        if (talismanInfo.get()) {
            if (name.contains("талисман") || name.contains("сфера") || name.contains("шар")) {
                if (name.contains("титан")) lines.add(Text.literal("§6✦ Талисман: §e+4 доп. сердца"));
                else if (name.contains("сил")) lines.add(Text.literal("§c✦ Сфера: §4+15% урона"));
                else if (name.contains("стойкост") || name.contains("защит")) lines.add(Text.literal("§9✦ Сфера: §b-10% получаемого урона"));
                else if (name.contains("скорост")) lines.add(Text.literal("§b✦ Сфера: §3+20% скорости"));
                else if (name.contains("феникс") || name.contains("возрожд")) lines.add(Text.literal("§e✦ Талисман: §6Второй шанс при смерти"));
                else if (name.contains("карател")) lines.add(Text.literal("§4✦ Талисман: §cКритический урон x1.5"));
                else lines.add(Text.literal("§b✦ HolyWorld: §3Особый артефакт"));
            }
        }

        if (potionTiers.get()) {
            net.minecraft.component.type.PotionContentsComponent pot = stack.get(net.minecraft.component.DataComponentTypes.POTION_CONTENTS);
            if (pot != null && pot.hasEffects()) {
                for (net.minecraft.entity.effect.StatusEffectInstance effect : pot.getEffects()) {
                    int durationSec = effect.getDuration() / 20;
                    int min = durationSec / 60;
                    int sec = durationSec % 60;
                    int amp = effect.getAmplifier() + 1;
                    lines.add(Text.literal("§d✦ Эффект: §f" + effect.getEffectType().value().getName().getString() + " " + amp + " §7(" + String.format(Locale.ROOT, "%02d:%02d", min, sec) + ")"));
                }
            }
        }

        if (repairCost.get()) {
            Integer cost = stack.get(net.minecraft.component.DataComponentTypes.REPAIR_COST);
            if (cost != null && cost > 0) {
                String warning = cost >= 39 ? " §c(Слишком дорого!)" : "";
                lines.add(Text.literal("§7[Починка наковальни] §e" + cost + " ур." + warning));
            }
        }

        if (enchantmentMax.get()) {
            net.minecraft.component.type.ItemEnchantmentsComponent enchs = stack.get(net.minecraft.component.DataComponentTypes.ENCHANTMENTS);
            if (enchs != null && !enchs.isEmpty()) {
                lines.add(Text.literal("§5✦ Зачарований: §d" + enchs.getSize()));
            }
        }

        if (compassTooltip.get()) {
            if (stack.isOf(Items.COMPASS) || name.contains("компас") || name.contains("радар")) {
                lines.add(Text.literal("§7[HolyWorld] §eБоевой радар поиска игроков (КД: 30с)"));
            }
        }
    }
}
