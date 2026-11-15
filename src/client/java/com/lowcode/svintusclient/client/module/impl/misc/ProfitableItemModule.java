package com.lowcode.svintusclient.client.module.impl.misc;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.TickListener;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ProfitableItemModule — авто-поиск и уведомление о топ-лотах аукциона HolyWorld.
 * Адаптировано из AuctionDealScanner (Doxaiz-visuals).
 */
public final class ProfitableItemModule extends Module implements TickListener {

    public final NumberSetting minProfit = add(new NumberSetting("Мин. выгода %", 15.0f, 5.0f, 100.0f, 5.0f));
    public final BooleanSetting notify = add(new BooleanSetting("Уведомление в чат", true));
    public final BooleanSetting playSound = add(new BooleanSetting("Звуковой сигнал", true));
    public final NumberSetting highlightGlow = add(new NumberSetting("Яркость подсветки", 0.8f, 0.2f, 1.0f, 0.05f));
    public final BooleanSetting showProfitMargin = add(new BooleanSetting("Показывать маржу %", true));

    private static final String[] DISPLAY_NAMES = {
            "Уникальный ключ", "ТНТ пушка", "Фармер", "Фермер", "Меч Цербера",
            "Рюкзак Инфинити", "Нерушимая мечта шахтёра",
            "Золотой спавнер с фармером", "Яйцо дракона"
    };

    private static final double[] DEFAULT_MAX_PRICES = {
            1500000.0, 800000.0, 500000.0, 500000.0, 2500000.0,
            1200000.0, 900000.0, 4500000.0, 2000000.0
    };

    private static final Pattern PRICE_PATTERN = Pattern.compile(
            "(?:цена|стоимость|price)[^0-9]{0,32}([0-9][0-9\\s.,]*\\s*[кkмm]?)",
            Pattern.CASE_INSENSITIVE);

    private final Set<String> notifiedLots = new HashSet<>();
    private Object lastScreen = null;

    public ProfitableItemModule() {
        super("Подсветка товаров", "Поиск и выделение лотов на аукционе с максимальной скидкой и выгодой", Category.HOLYWORLD);
    }

    @Override
    public void onTick() {
        if (!isEnabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        if (!(mc.currentScreen instanceof HandledScreen<?> screen)) {
            notifiedLots.clear();
            lastScreen = null;
            return;
        }

        String title = screen.getTitle().getString().toLowerCase(Locale.ROOT);
        if (!title.contains("аукцион") && !title.contains("auction")) {
            notifiedLots.clear();
            lastScreen = null;
            return;
        }

        if (lastScreen != screen) {
            lastScreen = screen;
            notifiedLots.clear();
        }

        for (Slot slot : screen.getScreenHandler().slots) {
            ItemStack stack = slot.getStack();
            if (stack == null || stack.isEmpty()) continue;

            if (isBargainLot(stack)) {
                String key = slot.id + ":" + stack.getName().getString() + ":" + stack.getCount();
                if (notifiedLots.add(key)) {
                    if (notify.get()) {
                        mc.player.sendMessage(Text.literal(
                                "§d[Выгодный товар] §f" + stack.getName().getString()
                                        + " §7— выгода: §a+" + (showProfitMargin.get() ? String.format(Locale.ROOT, "%.0f%%", minProfit.get()) : "дешевле рынка")), false);
                    }
                    if (playSound.get()) {
                        mc.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0f, 1.4f);
                    }
                }
            }
        }
    }

    public boolean isBargainLot(ItemStack stack) {
        int selectedType = getSelectedItem(stack);
        if (selectedType < 0) return false;

        String lore = getLoreText(stack);
        if (!hasNoBids(lore)) return false;

        double currentPrice = getPricePerItem(stack);
        double baseMaxPrice = selectedType < DEFAULT_MAX_PRICES.length ? DEFAULT_MAX_PRICES[selectedType] : 1000000.0;
        double discountFactor = 1.0 - (minProfit.get() / 100.0);
        double maxPrice = baseMaxPrice * Math.max(0.1, discountFactor);
        return currentPrice > 0.0D && currentPrice <= maxPrice;
    }

    private int getSelectedItem(ItemStack stack) {
        String name = normalize(stack.getName().getString());
        String data = name + " " + getLoreText(stack);
        if (isGoldenFarmerSpawner(stack)) return 7;
        if (stack.getItem() == Items.DRAGON_EGG || data.contains("яйцо дракона") || data.contains("dragon egg")) return 8;
        if (data.contains("уникальный ключ") || data.contains("unique key")) return 0;
        if (data.contains("тнт-пушка") || data.contains("тнт пушка") || data.contains("tnt cannon")) return 1;
        if (data.contains("меч цербера") || data.contains("cerberus sword")) return 4;
        if (data.contains("рюкзак инфинити") || data.contains("infinity backpack")) return 5;
        if (data.contains("нерушимая мечта шахт") || data.contains("unbreakable miner dream")) return 6;
        if (data.contains("фермер") || data.contains("farmer")) return 3;
        if (data.contains("фармер")) return 2;
        return -1;
    }

    private boolean isGoldenFarmerSpawner(ItemStack stack) {
        String name = normalize(stack.getName().getString());
        String lore = getLoreText(stack);
        boolean isSpawner = name.contains("золотой спавнер") || name.contains("golden spawner")
                || lore.contains("золотой спавнер") || lore.contains("golden spawner");
        boolean containsFarmer = lore.contains("фармер") || lore.contains("фермер") || lore.contains("farmer");
        return isSpawner && containsFarmer;
    }

    private boolean hasNoBids(String lore) {
        if (lore.contains("аукцион игроков")) return false;
        if (!lore.contains("ставк") && !lore.contains("bid")) return true;
        return lore.contains("ставок нет") || lore.contains("ставки отсутствуют")
                || lore.matches("(?s).*ставк\\w*\\s*[:\\-]?\\s*0(?:\\D.*)?")
                || lore.matches("(?s).*bids?\\s*[:\\-]?\\s*0(?:\\D.*)?");
    }

    private double getPricePerItem(ItemStack stack) {
        double lotPrice = getLotPrice(stack);
        return lotPrice / Math.max(1, stack.getCount());
    }

    private double getLotPrice(ItemStack stack) {
        LoreComponent loreComp = stack.get(DataComponentTypes.LORE);
        if (loreComp == null) return 0.0D;

        for (Text line : loreComp.lines()) {
            String text = normalize(line.getString());
            text = text.replaceAll("за\\s+\\d+\\s*(?:шт\\.?|штук[ауи]?)", "");
            Matcher matcher = PRICE_PATTERN.matcher(text);
            if (matcher.find()) {
                double amount = parseAmount(matcher.group(1));
                if (amount > 0.0D) return amount;
            }
        }
        return 0.0D;
    }

    private String getLoreText(ItemStack stack) {
        LoreComponent loreComp = stack.get(DataComponentTypes.LORE);
        if (loreComp == null) return "";

        StringBuilder sb = new StringBuilder();
        for (Text line : loreComp.lines()) {
            sb.append(' ').append(normalize(line.getString()));
        }
        return sb.toString();
    }

    private String normalize(String value) {
        return value.replaceAll("§[0-9a-fk-or]", "").toLowerCase(Locale.ROOT)
                .replace('ё', 'е');
    }

    private double parseAmount(String raw) {
        String value = raw.replace(" ", "").replace("\u00a0", "").toLowerCase(Locale.ROOT);
        double multiplier = 1.0D;
        if (value.endsWith("к") || value.endsWith("k")) {
            multiplier = 1_000.0D;
            value = value.substring(0, value.length() - 1);
        } else if (value.endsWith("м") || value.endsWith("m")) {
            multiplier = 1_000_000.0D;
            value = value.substring(0, value.length() - 1);
        }
        int dot = value.lastIndexOf('.');
        int comma = value.lastIndexOf(',');
        int decimalSeparator = Math.max(dot, comma);
        if (decimalSeparator >= 0 && value.length() - decimalSeparator - 1 <= 2) {
            String whole = value.substring(0, decimalSeparator).replace(".", "").replace(",", "");
            String fraction = value.substring(decimalSeparator + 1);
            value = whole + "." + fraction;
        } else {
            value = value.replace(".", "").replace(",", "");
        }
        try {
            return new BigDecimal(value).doubleValue() * multiplier;
        } catch (NumberFormatException exception) {
            return 0.0D;
        }
    }
}
