package com.lowcode.svintusclient.client.module;

import com.lowcode.svintusclient.client.module.client.ClickGuiModule;
import com.lowcode.svintusclient.client.module.client.ClientSoundsModule;
import com.lowcode.svintusclient.client.module.client.HudModule;
import com.lowcode.svintusclient.client.module.client.NotificationsModule;
import com.lowcode.svintusclient.client.module.impl.combat.*;
import com.lowcode.svintusclient.client.module.impl.cosmetics.*;
import com.lowcode.svintusclient.client.module.impl.misc.*;
import com.lowcode.svintusclient.client.module.impl.render.*;
import com.lowcode.svintusclient.client.module.impl.sounds.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ModuleManager {

    private static final List<Module> MODULES = new ArrayList<>();

    // Client & System
    public static final ClickGuiModule CLICK_GUI = register(new ClickGuiModule());
    public static final HudModule HUD = register(new HudModule());
    public static final NotificationsModule NOTIFICATIONS = register(new NotificationsModule());
    public static final ClientSoundsModule CLIENT_SOUNDS = register(new ClientSoundsModule());
    public static final HitSoundsModule HIT_SOUNDS = register(new HitSoundsModule());
    public static final KillSoundsModule KILL_SOUNDS = register(new KillSoundsModule());
    public static final TotemPopSoundModule TOTEM_POP_SOUNDS = register(new TotemPopSoundModule());
    public static final ValuableBreakSoundsModule VALUABLE_BREAK_SOUNDS = register(new ValuableBreakSoundsModule());

    // Render & Visuals
    public static final JumpCirclesModule JUMP_CIRCLES = register(new JumpCirclesModule());
    public static final ChinaHatModule CHINA_HAT = register(new ChinaHatModule());
    public static final BlockOverlayModule BLOCK_OVERLAY = register(new BlockOverlayModule());
    public static final TargetMarkerModule TARGET_MARKER = register(new TargetMarkerModule());
    public static final TrajectoryModule TRAJECTORY = register(new TrajectoryModule());
    public static final ViewModelModule VIEW_MODEL = register(new ViewModelModule());
    public static final SwingAnimationModule SWING_ANIMATION = register(new SwingAnimationModule());
    public static final FreeLookModule FREE_LOOK = register(new FreeLookModule());
    public static final AspectRatioModule ASPECT_RATIO = register(new AspectRatioModule());
    public static final FullbrightModule FULLBRIGHT = register(new FullbrightModule());
    public static final NoHurtCamModule NO_HURT_CAM = register(new NoHurtCamModule());
    public static final NoFogModule NO_FOG = register(new NoFogModule());
    public static final NoFireModule NO_FIRE = register(new NoFireModule());
    public static final TrailsModule TRAILS = register(new TrailsModule());
    public static final WorldParticlesModule WORLD_PARTICLES = register(new WorldParticlesModule());
    public static final CrosshairModule CROSSHAIR = register(new CrosshairModule());
    public static final ItemHighlightModule ITEM_HIGHLIGHT = register(new ItemHighlightModule());
    public static final CustomScoreboardModule SCOREBOARD = register(new CustomScoreboardModule());
    public static final ShulkerPreviewModule SHULKER_PREVIEW = register(new ShulkerPreviewModule());
    public static final ItemPhysicsModule ITEM_PHYSICS = register(new ItemPhysicsModule());
    public static final TntTimerModule TNT_TIMER = register(new TntTimerModule());
    public static final ChunkBordersModule CHUNK_BORDERS = register(new ChunkBordersModule());
    public static final WaypointsModule WAYPOINTS = register(new WaypointsModule());
    public static final BossBarsModule BOSS_BARS = register(new BossBarsModule());
    public static final CustomHotbarModule CUSTOM_HOTBAR = register(new CustomHotbarModule());
    public static final CustomSkyModule CUSTOM_SKY = register(new CustomSkyModule());
    public static final TabListModule TAB_LIST = register(new TabListModule());
    public static final ItemUseProgressModule ITEM_USE_PROGRESS = register(new ItemUseProgressModule());
    public static final ZoomModule ZOOM = register(new ZoomModule());
    public static final ChunksAnimationModule CHUNKS_ANIMATION = register(new ChunksAnimationModule());
    public static final MediaPlayerHudModule MEDIA_PLAYER = register(new MediaPlayerHudModule());
    public static final WatermarkHudModule WATERMARK_HUD = register(new WatermarkHudModule());
    public static final PotionsHudModule POTIONS_HUD = register(new PotionsHudModule());
    public static final CooldownsHudModule COOLDOWNS_HUD = register(new CooldownsHudModule());
    public static final KeybindsHudModule KEYBINDS_HUD = register(new KeybindsHudModule());
    public static final KeyStrokesHudModule KEY_STROKES = register(new KeyStrokesHudModule());
    public static final InventoryHudModule INVENTORY_HUD = register(new InventoryHudModule());
    public static final ItemPickupLogModule ITEM_PICKUP_LOG = register(new ItemPickupLogModule());
    public static final ItemDurabilityOverlayModule ITEM_DURABILITY = register(new ItemDurabilityOverlayModule());
    public static final ItemModelsModule ITEM_MODELS = register(new ItemModelsModule());
    public static final PerformanceModule PERFORMANCE = register(new PerformanceModule());
    public static final TotemGhostModule TOTEM_GHOST = register(new TotemGhostModule());

    // Combat & VFX
    public static final DamageIndicatorModule DAMAGE_INDICATOR = register(new DamageIndicatorModule());
    public static final HitParticlesModule HIT_PARTICLES = register(new HitParticlesModule());
    public static final HitColorModule HIT_COLOR = register(new HitColorModule());
    public static final CrownModule CROWN = register(new CrownModule());
    public static final KillEffectsModule KILL_EFFECTS = register(new KillEffectsModule());
    public static final TotemPopModule TOTEM_POP = register(new TotemPopModule());
    public static final MaceVisualsModule MACE_VISUALS = register(new MaceVisualsModule());

    // Cosmetics & Player
    public static final CosmeticsModule COSMETICS = register(new CosmeticsModule());

    // Misc
    public static final BetterMinecraftModule BETTER_MINECRAFT = register(new BetterMinecraftModule());

    // Batch 10: Complete Visual Port
    public static final TargetHudModule TARGET_HUD = register(new TargetHudModule());
    public static final CoordsHudModule COORDS_HUD = register(new CoordsHudModule());
    public static final RegionESPModule REGION_ESP = register(new RegionESPModule());

    // Batch 11: Final Source Expansion
    public static final NameTagsModule NAME_TAGS = register(new NameTagsModule());
    public static final StreamerModeModule STREAMER_MODE = register(new StreamerModeModule());
    public static final LockSlotModule LOCK_SLOT = register(new LockSlotModule());
    public static final CooldownTagsModule COOLDOWN_TAGS = register(new CooldownTagsModule());

    // Batch 12: 3D Cosmetics & Source Visuals
    public static final WingsCosmeticModule WINGS = register(new WingsCosmeticModule());
    public static final HatsCosmeticModule HATS = register(new HatsCosmeticModule());
    public static final BackWearCosmeticModule BACKWEAR = register(new BackWearCosmeticModule());
    public static final PetsCosmeticModule PETS = register(new PetsCosmeticModule());
    public static final TrinketModule TRINKET_AURAS = register(new TrinketModule());
    public static final AnimeEmotesModule ANIME_EMOTES = register(new AnimeEmotesModule());
    public static final AnimalEarsCosmeticModule ANIMAL_EARS = register(new AnimalEarsCosmeticModule());
    public static final AnimalTailCosmeticModule ANIMAL_TAIL = register(new AnimalTailCosmeticModule());
    public static final FriendHighlightModule FRIEND_HIGHLIGHT = register(new FriendHighlightModule());
    public static final MineViewerModule MINE_VIEWER = register(new MineViewerModule());

    // Batch 13: Aesthetic VFX & Atmosphere Expansion
    public static final BodyAuraModule BODY_AURA = register(new BodyAuraModule());
    public static final DivineHaloModule DIVINE_HALO = register(new DivineHaloModule());
    public static final ArmorInspectModule ARMOR_INSPECT = register(new ArmorInspectModule());
    public static final CustomSwordsModule CUSTOM_SWORDS = register(new CustomSwordsModule());
    public static final MarketHelperModule MARKET_HELPER = register(new MarketHelperModule());
    public static final PlayerMobModule PLAYER_MOB = register(new PlayerMobModule());

    // Batch 14: HolyWorld Expansion from Doxaiz-visuals
    public static final EventsModule EVENTS = register(new EventsModule());
    public static final CoinRateModule COIN_RATE = register(new CoinRateModule());
    public static final WipeTrackerModule WIPE_TRACKER = register(new WipeTrackerModule());
    public static final SpawnerContentsModule SPAWNER_CONTENTS = register(new SpawnerContentsModule());
    // Screen-shot targeted modules
    public static final CommandBindsModule COMMAND_BINDS = register(new CommandBindsModule());
    public static final TabPingModule TAB_PING = register(new TabPingModule());
    public static final FoodInfoModule FOOD_INFO = register(new FoodInfoModule());
    public static final ItemHintsModule ITEM_HINTS = register(new ItemHintsModule());
    public static final ProfitableItemModule PROFITABLE_ITEM = register(new ProfitableItemModule());
    public static final CompassCdModule COMPASS_CD = register(new CompassCdModule());
    public static final CoinPriceModule COIN_PRICE = register(new CoinPriceModule());

    // Batch 15: Kimiko & MVisuals Expansion
    public static final AutoSprintModule AUTO_SPRINT = register(new AutoSprintModule());
    public static final DeathCoordsModule DEATH_COORDS = register(new DeathCoordsModule());
    public static final FastExpModule FAST_EXP = register(new FastExpModule());

    // Batch 16: Kimiko & HolyWorld Pro Utility & Combat Modules
    public static final ClickPearlModule CLICK_PEARL = register(new ClickPearlModule());
    public static final AutoCommandsModule AUTO_COMMANDS = register(new AutoCommandsModule());


    private ModuleManager() {
    }

    public static void init() {
    }

    public static <T extends Module> T register(T module) {
        MODULES.add(module);
        return module;
    }

    public static List<Module> all() {
        return Collections.unmodifiableList(MODULES);
    }

    public static List<Module> byCategory(Category category) {
        return MODULES.stream().filter(m -> m.getCategory() == category).toList();
    }

    @SuppressWarnings("unchecked")
    public static <T extends Module> T get(Class<T> clazz) {
        for (Module m : MODULES) {
            if (clazz.isInstance(m)) return (T) m;
        }
        return null;
    }
}
