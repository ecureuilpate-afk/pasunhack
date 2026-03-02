package net.rominux.pasunhack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PasunhackConfig {
    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(),
            "pasunhack_autominer.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public List<String> whitelistedBlocks = new ArrayList<>();

    private static PasunhackConfig instance = null;

    public static PasunhackConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    public static PasunhackConfig load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                PasunhackConfig config = GSON.fromJson(reader, PasunhackConfig.class);
                if (config != null)
                    return config;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        PasunhackConfig config = new PasunhackConfig();
        config.whitelistedBlocks.add("minecraft:diorite");
        config.whitelistedBlocks.add("minecraft:diamond_ore");
        config.save();
        return config;
    }

    public void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
