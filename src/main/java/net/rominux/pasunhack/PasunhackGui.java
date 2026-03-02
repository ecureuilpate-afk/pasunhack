package net.rominux.pasunhack;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

public class PasunhackGui {

    public static Screen buildScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.literal("Configuration Pasunhack"));

        // Définit l'action à réaliser quand on clique sur "Save & Quit"
        builder.setSavingRunnable(PasunhackConfig::save);

        ConfigCategory general = builder.getOrCreateCategory(Text.literal("Général"));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        PasunhackConfig config = PasunhackConfig.getInstance();

        // Ajout de la liste de blocs à miner
        general.addEntry(entryBuilder.startStrList(Text.literal("Blocs à miner"), config.blocksToMine)
                .setDefaultValue(java.util.Arrays.asList("minecraft:diamond_ore", "minecraft:deepslate_diamond_ore", "minecraft:ancient_debris"))
                .setTooltip(Text.literal("Liste des IDs des blocs (ex: minecraft:diamond_ore)"))
                .setSaveConsumer(newValue -> config.blocksToMine = newValue)
                .build());

        // Ajout du champ pour la touche de bascule
        general.addEntry(entryBuilder.startKeyCodeField(Text.literal("Touche Toggle AutoMiner"), InputUtil.Type.KEYSYM.createFromCode(config.toggleKeyBinding))
                .setDefaultValue(InputUtil.Type.KEYSYM.createFromCode(org.lwjgl.glfw.GLFW.GLFW_KEY_F6))
                .setSaveConsumer(newValue -> config.toggleKeyBinding = newValue.getCode())
                .build());

        return builder.build();
    }
}
