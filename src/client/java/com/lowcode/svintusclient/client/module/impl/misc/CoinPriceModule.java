package com.lowcode.svintusclient.client.module.impl.misc;

import com.lowcode.svintusclient.client.holyworld.HolyWorldApiService;
import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Locale;

public final class CoinPriceModule extends Module {

    public final BooleanSetting showInTooltip = add(new BooleanSetting("Цена в подсказке", true));
    public final BooleanSetting autoConvertRub = add(new BooleanSetting("Пересчёт в RUB", true));
    public final BooleanSetting formatMillions = add(new BooleanSetting("Сокращать числа (к/кк)", true));
    public final BooleanSetting showExchangeRate = add(new BooleanSetting("Биржевой курс", true));

    public CoinPriceModule() {
        super("Цена в коинах", "Автоматический пересчёт стоимости предметов в коины по курсу биржи", Category.HOLYWORLD);
    }

    public void handleTooltip(ItemStack stack, List<Text> lines) {
        if (!isEnabled() || !showInTooltip.get() || stack == null || stack.isEmpty()) return;
        double rate = HolyWorldApiService.latestCoinRate;
        if (rate > 0) {
            if (!showExchangeRate.get()) return;
        if (autoConvertRub.get()) {
            String rateStr = (formatMillions.get() && rate >= 1000) ? String.format(Locale.ROOT, "%.1fk", rate / 1000.0) : String.format(Locale.ROOT, "%.2f", rate);
                lines.add(Text.literal("§6🪙 Курс коина: §e" + rateStr + " ₽"));
            } else {
                lines.add(Text.literal("§6🪙 Коины: §e" + HolyWorldApiService.coinRateFormatted + "$"));
            }
        }
    }
}
