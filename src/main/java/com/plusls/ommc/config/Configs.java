package com.plusls.ommc.config;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.plusls.ommc.ModInfo;
import com.plusls.ommc.feature.sortInventory.MyKeybindMulti;
import com.plusls.ommc.feature.sortInventory.SortInventoryUtil;
import com.plusls.ommc.gui.GuiConfigs;
import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.config.options.*;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Configs implements IConfigHandler {
    private static final String CONFIG_FILE_NAME = ModInfo.MOD_ID + ".json";
    private static final int CONFIG_VERSION = 1;
    private static final List<String> OLD_WORLD_EATER_MINE_HELPER_WHITELIST = new ArrayList<>();
    private static boolean firstLoadConfig = true;

    public static class Generic {
        private static final String PREFIX = String.format("%s.config.generic", ModInfo.MOD_ID);
        public static final ConfigHotkey OPEN_CONFIG_GUI = new TranslatableConfigHotkey(PREFIX, "openConfigGui", "O,C");
        public static final ConfigBoolean DEBUG = new TranslatableConfigBoolean(PREFIX, "debug", false);
        public static final ConfigBoolean DONT_CLEAR_CHAT_HISTORY = new TranslatableConfigBoolean(PREFIX, "dontClearChatHistory", false);
        public static final ConfigHotkey SORT_INVENTORY = new TranslatableConfigHotkey(PREFIX, "sortInventory", "R");
        public static final ConfigBoolean SORT_INVENTORY_SUPPORT_EMPTY_SHULKER_BOX_STACK = new TranslatableConfigBoolean(PREFIX, "sortInventorySupportEmptyShulkerBoxStack", false);

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                OPEN_CONFIG_GUI,
                DEBUG,
                DONT_CLEAR_CHAT_HISTORY,
                SORT_INVENTORY,
                SORT_INVENTORY_SUPPORT_EMPTY_SHULKER_BOX_STACK
        );

        public static final ImmutableList<ConfigHotkey> HOTKEYS = ImmutableList.of(
                OPEN_CONFIG_GUI,
                SORT_INVENTORY
        );


        static {
            OPEN_CONFIG_GUI.getKeybind().setCallback((keyAction, iKeybind) -> {
                GuiBase.openGui(new GuiConfigs());
                return true;
            });
            ((MyKeybindMulti) SORT_INVENTORY.getKeybind()).allowInScreen();
            SORT_INVENTORY.getKeybind().setCallback((keyAction, iKeybind) -> {
                SortInventoryUtil.sort();
                MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            });
            DEBUG.setValueChangeCallback(config -> {
                if (config.getBooleanValue()) {
                    Configurator.setLevel(ModInfo.LOGGER.getName(), Level.toLevel("DEBUG"));
                } else {
                    Configurator.setLevel(ModInfo.LOGGER.getName(), Level.toLevel("INFO"));
                }
            });
        }
    }

    public static class FeatureToggle {
        private static final String PREFIX = String.format("%s.config.feature_toggle", ModInfo.MOD_ID);
        public static final ConfigBooleanHotkeyed DISABLE_BREAK_SCAFFOLDING = new TranslatableConfigBooleanHotkeyed(PREFIX, "disableBreakScaffolding", false, "");
        public static final ConfigBooleanHotkeyed DISABLE_MOVE_DOWN_IN_SCAFFOLDING = new TranslatableConfigBooleanHotkeyed(PREFIX, "disableMoveDownInScaffolding", false, "");
        public static final ConfigBooleanHotkeyed FORCE_BREAKING_COOLDOWN = new TranslatableConfigBooleanHotkeyed(PREFIX, "forceBreakingCooldown", false, "");
        public static final ConfigBooleanHotkeyed HIGHLIGHT_LAVA_SOURCE = new TranslatableConfigBooleanHotkeyed(PREFIX, "highlightLavaSource", false, "");
        public static final ConfigBooleanHotkeyed HIGHLIGHT_WANDERING_TRADER = new TranslatableConfigBooleanHotkeyed(PREFIX, "highlightWanderingTrader", false, "");
        public static final ConfigBooleanHotkeyed PREVENT_EXPLODING_BED = new TranslatableConfigBooleanHotkeyed(PREFIX, "preventExplodingBed", false, "");
        public static final ConfigBooleanHotkeyed REAL_SNEAKING = new TranslatableConfigBooleanHotkeyed(PREFIX, "realSneaking", false, "");
        public static final ConfigBooleanHotkeyed WORLD_EATER_MINE_HELPER = new TranslatableConfigBooleanHotkeyed(PREFIX, "worldEaterMineHelper", false, "");

        public static final ImmutableList<ConfigBooleanHotkeyed> OPTIONS = ImmutableList.of(
                DISABLE_BREAK_SCAFFOLDING,
                DISABLE_MOVE_DOWN_IN_SCAFFOLDING,
                FORCE_BREAKING_COOLDOWN,
                HIGHLIGHT_LAVA_SOURCE,
                HIGHLIGHT_WANDERING_TRADER,
                PREVENT_EXPLODING_BED,
                REAL_SNEAKING,
                WORLD_EATER_MINE_HELPER
        );

        static {
            HIGHLIGHT_LAVA_SOURCE.setValueChangeCallback(config -> {
                ModInfo.LOGGER.debug("set HIGHLIGHT_LAVA_SOURCE {}", config.getBooleanValue());
                MinecraftClient.getInstance().worldRenderer.reload();
            });
            WORLD_EATER_MINE_HELPER.setValueChangeCallback(config -> {
                ModInfo.LOGGER.debug("set WORLD_EATER_MINE_HELPER {}", config.getBooleanValue());
                MinecraftClient.getInstance().worldRenderer.reload();
            });
        }
    }

    public static class Lists {
        private static final String PREFIX = String.format("%s.config.lists", ModInfo.MOD_ID);
        public static final ConfigStringList BREAK_SCAFFOLDING_WHITELIST = new TranslatableConfigStringList(PREFIX,
                "breakScaffoldingWhiteList", ImmutableList.of("minecraft:air", "minecraft:scaffolding"));
        public static final ConfigStringList MOVE_DOWN_IN_SCAFFOLDING_WHITELIST = new TranslatableConfigStringList(PREFIX,
                "moveDownInScaffoldingWhiteList", ImmutableList.of("minecraft:air", "minecraft:scaffolding"));
        public static final ConfigStringList WORLD_EATER_MINE_HELPER_WHITELIST = new TranslatableConfigStringList(PREFIX,
                "worldEaterMineHelperWhitelist", ImmutableList.of("_ore", "minecraft:ancient_debris", "minecraft:obsidian"));
        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                BREAK_SCAFFOLDING_WHITELIST,
                MOVE_DOWN_IN_SCAFFOLDING_WHITELIST,
                WORLD_EATER_MINE_HELPER_WHITELIST
        );

    }

    public static class AdvancedIntegratedServer {
        private static final String PREFIX = String.format("%s.config.advanced_integrated_server", ModInfo.MOD_ID);
        public static final ConfigBoolean ONLINE_MODE = new TranslatableConfigBoolean(PREFIX, "onlineMode", true);
        public static final ConfigBoolean PVP = new TranslatableConfigBoolean(PREFIX, "pvp", true);
        public static final ConfigBoolean FLIGHT = new TranslatableConfigBoolean(PREFIX, "flight", true);
        public static final ConfigInteger PORT = new TranslatableConfigInteger(PREFIX, "port", 0, 0, 65535);

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                ONLINE_MODE,
                PVP,
                FLIGHT,
                PORT
        );

        static {
            ONLINE_MODE.setValueChangeCallback(config -> {
                ModInfo.LOGGER.debug("set ONLINE_MODE {}", config.getBooleanValue());
                if (MinecraftClient.getInstance().isIntegratedServerRunning()) {
                    Objects.requireNonNull(MinecraftClient.getInstance().getServer()).setOnlineMode(ONLINE_MODE.getBooleanValue());
                }
            });
            PVP.setValueChangeCallback(config -> {
                ModInfo.LOGGER.debug("set PVP {}", config.getBooleanValue());
                if (MinecraftClient.getInstance().isIntegratedServerRunning()) {
                    Objects.requireNonNull(MinecraftClient.getInstance().getServer()).setPvpEnabled(PVP.getBooleanValue());
                }
            });
            FLIGHT.setValueChangeCallback(config -> {
                ModInfo.LOGGER.debug("set FLIGHT {}", config.getBooleanValue());
                if (MinecraftClient.getInstance().isIntegratedServerRunning()) {
                    Objects.requireNonNull(MinecraftClient.getInstance().getServer()).setFlightEnabled(PVP.getBooleanValue());
                }
            });
        }

    }

    public static void updateOldStringList() {
        OLD_WORLD_EATER_MINE_HELPER_WHITELIST.clear();
        OLD_WORLD_EATER_MINE_HELPER_WHITELIST.addAll(Lists.WORLD_EATER_MINE_HELPER_WHITELIST.getStrings());
    }

    public static void checkIsStringListChanged() {
        if (OLD_WORLD_EATER_MINE_HELPER_WHITELIST.size() > Lists.WORLD_EATER_MINE_HELPER_WHITELIST.getStrings().size()) {
            MinecraftClient.getInstance().worldRenderer.reload();
            updateOldStringList();
            return;
        }
        for (String string : Lists.WORLD_EATER_MINE_HELPER_WHITELIST.getStrings()) {
            if (!OLD_WORLD_EATER_MINE_HELPER_WHITELIST.contains(string)) {
                MinecraftClient.getInstance().worldRenderer.reload();
                updateOldStringList();
                return;
            }
        }
    }


    public static void loadFromFile() {
        File configFile = new File(FileUtils.getConfigDirectory(), CONFIG_FILE_NAME);

        if (configFile.exists() && configFile.isFile() && configFile.canRead()) {
            JsonElement element = JsonUtils.parseJsonFile(configFile);

            if (element != null && element.isJsonObject()) {
                JsonObject root = element.getAsJsonObject();
                ConfigUtils.readConfigBase(root, "Generic", Generic.OPTIONS);
                ConfigUtils.readHotkeyToggleOptions(root, "FeatureHotkey", "FeatureToggle", FeatureToggle.OPTIONS);
                ConfigUtils.readConfigBase(root, "Lists", Lists.OPTIONS);
                ConfigUtils.readConfigBase(root, "AdvancedIntegratedServer", AdvancedIntegratedServer.OPTIONS);
                int version = JsonUtils.getIntegerOrDefault(root, "config_version", 0);
            }
        }
        if (Generic.DEBUG.getBooleanValue()) {
            Configurator.setLevel(ModInfo.LOGGER.getName(), Level.toLevel("DEBUG"));
        }
        if (firstLoadConfig) {
            updateOldStringList();
        }
        firstLoadConfig = false;
    }

    public static void saveToFile() {
        File dir = FileUtils.getConfigDirectory();

        if ((dir.exists() && dir.isDirectory()) || dir.mkdirs()) {
            JsonObject root = new JsonObject();
            ConfigUtils.writeConfigBase(root, "Generic", Generic.OPTIONS);
            ConfigUtils.writeHotkeyToggleOptions(root, "FeatureHotkey", "FeatureToggle", FeatureToggle.OPTIONS);
            ConfigUtils.writeConfigBase(root, "Lists", Lists.OPTIONS);
            ConfigUtils.writeConfigBase(root, "AdvancedIntegratedServer", AdvancedIntegratedServer.OPTIONS);
            root.add("config_version", new JsonPrimitive(CONFIG_VERSION));
            JsonUtils.writeJsonToFile(root, new File(dir, CONFIG_FILE_NAME));
        }
    }

    @Override
    public void load() {
        loadFromFile();
    }

    @Override
    public void save() {
        saveToFile();
    }
}