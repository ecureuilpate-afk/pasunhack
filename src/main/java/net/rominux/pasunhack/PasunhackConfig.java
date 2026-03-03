package net.rominux.pasunhack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PasunhackConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "pasunhack.json");

    public List<String> blocksToMine = new ArrayList<>(Arrays.asList(
            "minecraft:diamond_ore",
            "minecraft:deepslate_diamond_ore",
            "minecraft:ancient_debris"));
    public int toggleKeyBinding = GLFW.GLFW_KEY_F6;

    public boolean autoPrecisionMiner = false;
    public boolean showCommissionsHud = true;
    public boolean showPickobulusPreview = true;
    public boolean showCommissionWaypoints = true;
    public boolean solveFetchur = true;
    public boolean solvePuzzler = true;
    public boolean titaniumTracer = false;
    public int titaniumTracerRadius = 30;

    private static PasunhackConfig instance;

    public static PasunhackConfig getInstance() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    public static void load() {
        if (FILE.exists()) {
            try (FileReader reader = new FileReader(FILE)) {
                instance = GSON.fromJson(reader, PasunhackConfig.class);
            } catch (IOException e) {
                instance = new PasunhackConfig();
                e.printStackTrace();
            }
        } else {
            instance = new PasunhackConfig();
            save();
        }
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(FILE)) {
            GSON.toJson(instance, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
