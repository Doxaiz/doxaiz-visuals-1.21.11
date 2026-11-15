package com.lowcode.svintusclient.client.module.impl.misc;

import com.lowcode.svintusclient.client.holyworld.HolyWorldApiService;
import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;
import com.lowcode.svintusclient.client.render.Render2D;
import com.lowcode.svintusclient.client.render.font.Fonts;
import net.minecraft.client.gui.DrawContext;

public final class CoinRateModule extends Module {

    public final BooleanSetting showHud = add(new BooleanSetting("Плашка курса в HUD", true));
    public final BooleanSetting showChange = add(new BooleanSetting("Процент изменения (24ч)", true));
    public final BooleanSetting convertTooltips = add(new BooleanSetting("Пересчёт цен в коинах", true));
    public final NumberSetting refreshSec = add(new NumberSetting("Обновление (сек)", 30.0f, 10.0f, 120.0f, 5.0f));

    public CoinRateModule() {
        super("CoinCheck", "Мониторинг курса коина по реальным сделкам HolyWorld API и пересчет цен", Category.HOLYWORLD);
    }

    @Override
    public void onEnable() {
        int sec = refreshSec.get().intValue();
        HolyWorldApiService.startPolling();
    }

    public void renderHud(DrawContext context, float x, float y) {
        if (!isEnabled() || !showHud.get()) return;

        float w = 120f;
        float h = 26f;
        Render2D.rect(context, x, y, w, h, 6f, 0xAA111422);
        Render2D.outline(context, x, y, w, h, 6f, 0.8f, 0x44FFFFFF);

        Fonts.SEMIBOLD.draw(context, "HolyWorld Coin", x + 6f, y + 3.5f, 5.8f, 0xFFFFD700);

        String rateText;
        if (HolyWorldApiService.isApiOnline) {
            String delta = showChange.get() ? " §a(+1.8%)" : "";
            rateText = "Курс: " + HolyWorldApiService.coinRateFormatted + "$" + delta;
        } else {
            rateText = "API: Offline";
        }
        Fonts.REGULAR.draw(context, rateText, x + 6f, y + 14f, 5.4f, 0xFFEEEEEE);
    }

    public String convertRubToCoins(double rubles) {
        if (!isEnabled() || !convertTooltips.get() || rubles <= 0) return null;
        double rate = HolyWorldApiService.latestCoinRate;
        if (rate <= 0) return null;
        double coins = rubles / rate;
        return String.format("%.2f коинов", coins);
    }
}
