package com.lowcode.svintusclient.client.module.impl.misc;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.TickListener;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

public final class ClickPearlModule extends Module implements TickListener {

    public final NumberSetting swapDelayMs = add(new NumberSetting("Задержка возврата (мс)", 50.0f, 20.0f, 300.0f, 10.0f));
    public final BooleanSetting switchBack = add(new BooleanSetting("Возврат в прежний слот", true));
    public final BooleanSetting middleClick = add(new BooleanSetting("Бросок на СКМ (Колёсико)", true));
    public final BooleanSetting invSearch = add(new BooleanSetting("Искать в инвентаре", true));
    public final BooleanSetting notifyChat = add(new BooleanSetting("Оповещение в чат", true));
    public final BooleanSetting soundAlert = add(new BooleanSetting("Звук броска", true));
    public final com.lowcode.svintusclient.client.module.setting.ModeSetting sound = add(new com.lowcode.svintusclient.client.module.setting.ModeSetting("Звук", "AimBooster", "Pop", "Ding", "Metallic", "Без звука"));

    private enum State {
        IDLE,
        WAIT_BEFORE_SWAP,
        WAIT_BEFORE_RESTORE
    }

    private State state = State.IDLE;
    private long actionTimer = 0L;
    private int previousSlot = -1;
    private int pearlSlot = -1;
    private long lastThrowTime = 0L;

    public ClickPearlModule() {
        super("ClickPearl", "Быстрый бросок эндер-жемчуга из хотбара по бинду без открытия инвентаря", Category.UTILITIES);
    }

    @Override
    protected void onEnable() {
        trigger();
        setEnabled(false);
    }

    public static void onMiddleClick() {
        ClickPearlModule cp = com.lowcode.svintusclient.client.module.ModuleManager.get(ClickPearlModule.class);
        if (cp != null && cp.middleClick.get()) {
            cp.trigger();
        }
    }

    /**
     * Triggered directly by keybinding, middle click, or module bind toggle.
     */
    public void trigger() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.currentScreen != null) return;

        if (System.currentTimeMillis() - lastThrowTime < 250L) return;
        if (mc.player.getItemCooldownManager().isCoolingDown(Items.ENDER_PEARL.getDefaultStack())) {
            return;
        }

        int slot = findPearlInHotbar(mc);
        if (slot == -1 && invSearch.get() && mc.interactionManager != null) {
            slot = swapFromInventory(mc);
        }

        if (slot == -1) {
            if (notifyChat.get()) {
                mc.player.sendMessage(Text.literal("§c[ClickPearl] §fЭндер-жемчуг не найден в хотбаре!"), false);
            }
            return;
        }

        this.previousSlot = mc.player.getInventory().getSelectedSlot();
        this.pearlSlot = slot;
        this.state = State.WAIT_BEFORE_SWAP;
        this.actionTimer = System.currentTimeMillis();
    }

    private int findPearlInHotbar(MinecraftClient mc) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.ENDER_PEARL)) {
                return i;
            }
        }
        return -1;
    }

    private int swapFromInventory(MinecraftClient mc) {
        for (int i = 9; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.ENDER_PEARL)) {
                int targetHotbar = mc.player.getInventory().getSelectedSlot();
                // Quick swap via pickup and slot click
                int syncId = mc.player.currentScreenHandler.syncId;
                mc.interactionManager.clickSlot(syncId, i, targetHotbar, net.minecraft.screen.slot.SlotActionType.SWAP, mc.player);
                return targetHotbar;
            }
        }
        return -1;
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null) {
            resetState();
            return;
        }

        long now = System.currentTimeMillis();
        long delay = (long) swapDelayMs.get().floatValue();

        if (state == State.WAIT_BEFORE_SWAP) {
            mc.player.getInventory().setSelectedSlot(pearlSlot);
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            mc.player.swingHand(Hand.MAIN_HAND);
            lastThrowTime = now;
            if (notifyChat.get()) {
                mc.player.sendMessage(Text.literal("§d[ClickPearl] §fЖемчуг брошен!"), true);
            }
            if (soundAlert.get() && !"Без звука".equals(sound.get())) {
                com.lowcode.svintusclient.client.util.SoundPreview.playPreview(sound.get(), 0.85f);
            }
            state = State.WAIT_BEFORE_RESTORE;
            actionTimer = now;
        } else if (state == State.WAIT_BEFORE_RESTORE) {
            if (now - actionTimer >= delay) {
                if (switchBack.get() && previousSlot >= 0 && previousSlot < 9) {
                    mc.player.getInventory().setSelectedSlot(previousSlot);
                }
                resetState();
            }
        }
    }

    private void resetState() {
        this.state = State.IDLE;
        this.actionTimer = 0L;
        this.previousSlot = -1;
        this.pearlSlot = -1;
    }

    @Override
    protected void onDisable() {
        resetState();
    }
}
