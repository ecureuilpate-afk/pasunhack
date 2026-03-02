package net.rominux.pasunhack;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.text.Text;
import java.util.List;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Text.literal("Pasunhack AutoMiner Config"));

            ConfigCategory general = builder.getOrCreateCategory(Text.literal("General"));
            ConfigEntryBuilder entryBuilder = builder.entryBuilder();

            general.addEntry(entryBuilder
                    .startStrList(Text.literal("Whitelisted Blocks"), PasunhackConfig.get().whitelistedBlocks)
                    .setDefaultValue(List.of("minecraft:diorite", "minecraft:diamond_ore"))
                    .setTooltip(Text.literal("Block IDs to mine (e.g. minecraft:stone)"))
                    .setSaveConsumer(newValue -> PasunhackConfig.get().whitelistedBlocks = newValue)
                    .build());

            builder.setSavingRunnable(() -> PasunhackConfig.get().save());

            return builder.build();
        };
    }
}
