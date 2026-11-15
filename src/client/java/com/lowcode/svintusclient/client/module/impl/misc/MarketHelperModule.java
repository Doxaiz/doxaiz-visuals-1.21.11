package com.lowcode.svintusclient.client.module.impl.misc;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ColorSetting;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MarketHelperModule extends Module {

    public final BooleanSetting highlightCheap = add(new BooleanSetting("Подсветка выгодных цен", true));
    public final ColorSetting cheapColor = add(new ColorSetting("Цвет подсветки", 0xFF2ECC71));
    public final BooleanSetting pricePerUnit = add(new BooleanSetting("Цена за штуку в стаке", true));
    public final BooleanSetting showTotalItems = add(new BooleanSetting("Количество предметов в лоте", true));
    public final BooleanSetting profitHighlight = add(new BooleanSetting("Выгодные лоты ниже рынка", true));
    public final BooleanSetting quickBuyHint = add(new BooleanSetting("Подсказка быстрого выкупа", true));
    public final BooleanSetting soundAlert = add(new BooleanSetting("Звуковое оповещение", true));
    public final BooleanSetting antiScam = add(new BooleanSetting("Защита от оверпрайса", true));

    public final ModeSetting targetCategory = add(new ModeSetting("Режим фильтрации",
            "Выбор по списку ниже", "Все товары", "ТНТ пушка", "Фармер / Фермер", "Золотой спавнер", "Меч Цербера", "Уникальные ключи", "Мечта шахтёра", "Рюкзак Инфинити", "Яйцо дракона", "Сферы и талисманы"));

    // Item selection filters from HolyWorld & examples
    public final BooleanSetting filterTnt = add(new BooleanSetting("ТНТ пушка", true));
    public final BooleanSetting filterFarmer = add(new BooleanSetting("Фармер / Фермер", true));
    public final BooleanSetting filterSpawner = add(new BooleanSetting("Золотой спавнер с фермером", true));
    public final BooleanSetting filterCerberus = add(new BooleanSetting("Меч Цербера", true));
    public final BooleanSetting filterKey = add(new BooleanSetting("Уникальный ключ", true));
    public final BooleanSetting filterMinerDream = add(new BooleanSetting("Мечта шахтёра", true));
    public final BooleanSetting filterBackpack = add(new BooleanSetting("Рюкзак Инфинити", true));
    public final BooleanSetting filterDragonEgg = add(new BooleanSetting("Яйцо дракона", true));
    public final BooleanSetting filterTalismans = add(new BooleanSetting("Сферы и талисманы", true));

    private static final Pattern PRICE_PATTERN = Pattern.compile(
            "(?:цена|стоимость|price)[^0-9]{0,32}([0-9][0-9\\s.,]*\\s*[кkмm]?)",
            Pattern.CASE_INSENSITIVE
    );

    public MarketHelperModule() {
        super("Аукцион Хелпер", "Аукционный помощник (/ah): подсветка выгодных лотов, анализ рынка и расчет цены за 1 штуку", Category.HOLYWORLD);
    }

    public double extractPrice(List<Text> tooltip) {
        if (tooltip == null || tooltip.isEmpty()) return -1.0;
        for (Text line : tooltip) {
            String str = line.getString();
            Matcher matcher = PRICE_PATTERN.matcher(str);
            if (matcher.find()) {
                String raw = matcher.group(1).replace(" ", "").replace(",", ".").toLowerCase();
                double multiplier = 1.0;
                if (raw.endsWith("к") || raw.endsWith("k")) {
                    multiplier = 1_000.0;
                    raw = raw.substring(0, raw.length() - 1);
                } else if (raw.endsWith("м") || raw.endsWith("m")) {
                    multiplier = 1_000_000.0;
                    raw = raw.substring(0, raw.length() - 1);
                }
                try {
                    return Double.parseDouble(raw) * multiplier;
                } catch (NumberFormatException ignored) {}
            }
        }
        return -1.0;
    }

    public boolean matchesItemFilter(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        String name = stack.getName().getString().toLowerCase();
        StringBuilder allText = new StringBuilder(name);

        var lore = stack.get(net.minecraft.component.DataComponentTypes.LORE);
        if (lore != null) {
            for (Text t : lore.lines()) {
                allText.append(" ").append(t.getString().toLowerCase());
            }
        }
        var customData = stack.get(net.minecraft.component.DataComponentTypes.CUSTOM_DATA);
        if (customData != null) {
            allText.append(" ").append(customData.copyNbt().toString().toLowerCase());
        }
        String text = allText.toString();

        if (filterTnt.get() && (text.contains("тнт-пушка") || text.contains("тнт пушка") || text.contains("tnt cannon") || text.contains("пушка"))) return true;
        if (filterFarmer.get() && (text.contains("фармер") || text.contains("фермер") || text.contains("farmer"))) return true;
        if (filterSpawner.get() && (text.contains("спавнер") || text.contains("spawner")) && (text.contains("фармер") || text.contains("фермер") || text.contains("farmer") || text.contains("золотой"))) return true;
        if (filterCerberus.get() && (text.contains("цербер") || text.contains("cerberus"))) return true;
        if (filterKey.get() && (text.contains("уникальный ключ") || text.contains("unique key") || (text.contains("ключ") && text.contains("holy")))) return true;
        if (filterMinerDream.get() && (text.contains("мечта шахт") || text.contains("miner dream"))) return true;
        if (filterBackpack.get() && (text.contains("инфинити") || text.contains("infinity"))) return true;
        if (filterDragonEgg.get() && (text.contains("дракон") || text.contains("dragon egg"))) return true;
        if (filterTalismans.get() && (text.contains("сфер") || text.contains("талисман") || text.contains("talisman"))) return true;

        return false;
    }

    public boolean isCheapLot(ItemStack stack) {
        if (!isEnabled() || stack == null || stack.isEmpty()) return false;
        String cat = targetCategory.get();
        if ("Все товары".equals(cat)) return true;
        if ("Выбор по списку ниже".equals(cat)) {
            return matchesItemFilter(stack);
        }

        String name = stack.getName().getString().toLowerCase();
        StringBuilder allText = new StringBuilder(name);
        var lore = stack.get(net.minecraft.component.DataComponentTypes.LORE);
        if (lore != null) {
            for (Text t : lore.lines()) {
                allText.append(" ").append(t.getString().toLowerCase());
            }
        }
        var customData = stack.get(net.minecraft.component.DataComponentTypes.CUSTOM_DATA);
        if (customData != null) {
            allText.append(" ").append(customData.copyNbt().toString().toLowerCase());
        }
        String text = allText.toString();

        if ("ТНТ пушка".equals(cat)) {
            return text.contains("тнт-пушка") || text.contains("тнт пушка") || text.contains("tnt cannon") || text.contains("пушка");
        } else if ("Фармер / Фермер".equals(cat)) {
            return text.contains("фармер") || text.contains("фермер") || text.contains("farmer");
        } else if ("Золотой спавнер".equals(cat)) {
            return (text.contains("спавнер") || text.contains("spawner")) && (text.contains("фармер") || text.contains("фермер") || text.contains("золотой"));
        } else if ("Меч Цербера".equals(cat)) {
            return text.contains("цербер") || text.contains("cerberus");
        } else if ("Уникальные ключи".equals(cat)) {
            return text.contains("уникальный ключ") || text.contains("unique key") || (text.contains("ключ") && text.contains("holy"));
        } else if ("Мечта шахтёра".equals(cat)) {
            return text.contains("мечта шахт") || text.contains("miner dream");
        } else if ("Рюкзак Инфинити".equals(cat)) {
            return text.contains("инфинити") || text.contains("infinity");
        } else if ("Яйцо дракона".equals(cat)) {
            return text.contains("дракон") || text.contains("dragon egg");
        } else if ("Сферы и талисманы".equals(cat)) {
            return text.contains("сфер") || text.contains("талисман") || text.contains("talisman");
        }
        return true;
    }

    public String formatUnitPrice(double totalPrice, int count) {
        if (totalPrice <= 0 || count <= 1) return null;
        double perUnit = totalPrice / count;
        if (perUnit >= 1_000_000.0) {
            return String.format("%.2fM / шт", perUnit / 1_000_000.0);
        } else if (perUnit >= 1_000.0) {
            return String.format("%.1fK / шт", perUnit / 1_000.0);
        } else {
            return String.format("%.0f$ / шт", perUnit);
        }
    }

    public void handleTooltip(ItemStack stack, List<Text> tooltip) {
        if (!isEnabled() || stack == null || tooltip == null) return;

        double price = extractPrice(tooltip);
        if (pricePerUnit.get() && price > 0 && stack.getCount() > 1) {
            String unitPrice = formatUnitPrice(price, stack.getCount());
            if (unitPrice != null) {
                tooltip.add(Text.literal("§a💰 " + unitPrice));
            }
        }

        if (showTotalItems.get() && stack.getCount() > 1) {
            tooltip.add(Text.literal("§7В лоте: §f" + stack.getCount() + " шт."));
        }

        if (profitHighlight.get() && isCheapLot(stack)) {
            tooltip.add(Text.literal("§6★ Выгодное предложение рынка"));
        }

        if (antiScam.get() && price > 500_000.0 && stack.getCount() == 1) {
            String itemName = stack.getName().getString().toLowerCase();
            if (itemName.contains("булыжник") || itemName.contains("грязь") || itemName.contains("палка")) {
                tooltip.add(Text.literal("§c⚠ Внимание: Возможный оверпрайс!"));
            }
        }

        if (quickBuyHint.get() && price > 0) {
            tooltip.add(Text.literal("§e[ЛКМ] §7— Быстрый выкуп лота"));
        }
    }
}
