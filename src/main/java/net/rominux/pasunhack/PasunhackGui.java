package net.rominux.pasunhack;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PasunhackGui extends Screen {

        private final Screen parent;
        private final PasunhackConfig config;

        private TextFieldWidget blockInputField;
        private ButtonWidget toggleKeyButton;

        private boolean isListeningForKey = false;

        private static class SearchResult {
                Block block;
                ButtonWidget button;

                SearchResult(Block block, ButtonWidget button) {
                        this.block = block;
                        this.button = button;
                }
        }

        private final List<SearchResult> searchResults = new ArrayList<>();
        private final List<ButtonWidget> removeButtons = new ArrayList<>();

        public PasunhackGui(Screen parent) {
                super(Text.literal("Configuration Pasunhack"));
                this.parent = parent;
                this.config = PasunhackConfig.getInstance();
        }

        @Override
        protected void init() {
                int centerX = this.width / 2;
                int centerY = this.height / 2;

                this.clearChildren();

                // Toggle Keybind Button
                this.toggleKeyButton = ButtonWidget.builder(getKeybindText(), button -> {
                        this.isListeningForKey = true;
                        button.setMessage(Text.literal("> Appuyez sur une touche <"));
                }).dimensions(centerX - 100, 40, 200, 20).build();
                this.addDrawableChild(this.toggleKeyButton);

                int rX = centerX + 110;
                this.addDrawableChild(ButtonWidget
                                .builder(Text.literal("Precision: " + (config.autoPrecisionMiner ? "ON" : "OFF")),
                                                btn -> {
                                                        config.autoPrecisionMiner = !config.autoPrecisionMiner;
                                                        btn.setMessage(Text.literal("Precision: "
                                                                        + (config.autoPrecisionMiner ? "ON" : "OFF")));
                                                })
                                .dimensions(rX, 40, 130, 20).build());

                this.addDrawableChild(ButtonWidget
                                .builder(Text.literal("HUD Comms: " + (config.showCommissionsHud ? "ON" : "OFF")),
                                                btn -> {
                                                        config.showCommissionsHud = !config.showCommissionsHud;
                                                        btn.setMessage(Text.literal("HUD Comms: "
                                                                        + (config.showCommissionsHud ? "ON" : "OFF")));
                                                })
                                .dimensions(rX, 65, 130, 20).build());

                this.addDrawableChild(ButtonWidget
                                .builder(Text.literal("Pickobulus: " + (config.showPickobulusPreview ? "ON" : "OFF")),
                                                btn -> {
                                                        config.showPickobulusPreview = !config.showPickobulusPreview;
                                                        btn.setMessage(Text.literal("Pickobulus: "
                                                                        + (config.showPickobulusPreview ? "ON"
                                                                                        : "OFF")));
                                                })
                                .dimensions(rX, 90, 130, 20).build());

                this.addDrawableChild(ButtonWidget
                                .builder(Text.literal("Waypoints: " + (config.showCommissionWaypoints ? "ON" : "OFF")),
                                                btn -> {
                                                        config.showCommissionWaypoints = !config.showCommissionWaypoints;
                                                        btn.setMessage(Text.literal("Waypoints: "
                                                                        + (config.showCommissionWaypoints ? "ON"
                                                                                        : "OFF")));
                                                })
                                .dimensions(rX, 115, 130, 20).build());

                this.addDrawableChild(ButtonWidget
                                .builder(Text.literal("Solve Fetchur: " + (config.solveFetchur ? "ON" : "OFF")),
                                                btn -> {
                                                        config.solveFetchur = !config.solveFetchur;
                                                        btn.setMessage(Text.literal("Solve Fetchur: "
                                                                        + (config.solveFetchur ? "ON"
                                                                                        : "OFF")));
                                                })
                                .dimensions(rX, 140, 130, 20).build());

                this.addDrawableChild(ButtonWidget
                                .builder(Text.literal("Solve Puzzler: " + (config.solvePuzzler ? "ON" : "OFF")),
                                                btn -> {
                                                        config.solvePuzzler = !config.solvePuzzler;
                                                        btn.setMessage(Text.literal("Solve Puzzler: "
                                                                        + (config.solvePuzzler ? "ON"
                                                                                        : "OFF")));
                                                })
                                .dimensions(rX, 165, 130, 20).build());

                this.addDrawableChild(ButtonWidget
                                .builder(Text.literal("Titanium Tracer: " + (config.titaniumTracer ? "ON" : "OFF")),
                                                btn -> {
                                                        config.titaniumTracer = !config.titaniumTracer;
                                                        btn.setMessage(Text.literal("Titanium Tracer: "
                                                                        + (config.titaniumTracer ? "ON"
                                                                                        : "OFF")));
                                                })
                                .dimensions(rX, 190, 130, 20).build());

                this.addDrawableChild(ButtonWidget
                                .builder(Text.literal("Tracer Radius: " + config.titaniumTracerRadius),
                                                btn -> {
                                                        config.titaniumTracerRadius += 10;
                                                        if (config.titaniumTracerRadius > 100)
                                                                config.titaniumTracerRadius = 10;
                                                        btn.setMessage(Text.literal("Tracer Radius: "
                                                                        + config.titaniumTracerRadius));
                                                })
                                .dimensions(rX, 215, 130, 20).build());

                this.addDrawableChild(ButtonWidget
                                .builder(Text.literal("Dump TabList -> Logs"),
                                                btn -> {
                                                        dumpTablistLogs();
                                                        btn.setMessage(Text.literal("Dumped !"));
                                                })
                                .dimensions(rX, 240, 130, 20).build());

                // Block Input Field
                this.blockInputField = new TextFieldWidget(this.textRenderer, centerX - 100, 80, 200, 20,
                                Text.literal("Chercher un bloc..."));
                this.blockInputField.setMaxLength(100);
                this.blockInputField.setChangedListener(this::updateSearchResults);
                this.addDrawableChild(this.blockInputField);

                this.searchResults.clear();
                this.removeButtons.clear();
                updateSearchResults("");

                // Save & Quit
                ButtonWidget saveAndQuit = ButtonWidget.builder(Text.literal("Sauvegarder et Quitter"), button -> {
                        PasunhackConfig.save();
                        this.client.setScreen(this.parent);
                }).dimensions(centerX - 100, this.height - 30, 200, 20).build();
                this.addDrawableChild(saveAndQuit);
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
                context.fill(0, 0, this.width, this.height, 0x90000000);
                context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 15, 0xFFFFFF);
                super.render(context, mouseX, mouseY, delta);

                for (SearchResult res : this.searchResults) {
                        int x = res.button.getX() + 4;
                        int y = res.button.getY() + 2;
                        context.drawItem(new ItemStack(res.block), x, y);
                }

                if (this.blockInputField.getText().isEmpty()) {
                        int centerX = this.width / 2;
                        int listY = 110;
                        int index = 0;
                        for (String blockId : this.config.blocksToMine) {
                                try {
                                        Identifier id = Identifier.of(blockId);
                                        if (Registries.BLOCK.containsId(id)) {
                                                Block b = Registries.BLOCK.get(id);
                                                int x = centerX - 100 + 4;
                                                int y = listY + (index * 24) + 2;
                                                context.drawItem(new ItemStack(b), x, y);
                                        }
                                } catch (Exception e) {
                                        // Ignore if blockId is malformed
                                }
                                index++;
                        }
                }
        }

        @Override
        public boolean keyPressed(KeyInput input) {
                if (this.isListeningForKey) {
                        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
                                this.config.toggleKeyBinding = InputUtil.UNKNOWN_KEY.getCode();
                        } else {
                                this.config.toggleKeyBinding = input.key();
                        }
                        this.isListeningForKey = false;
                        this.toggleKeyButton.setMessage(getKeybindText());
                        PasunhackConfig.save();
                        return true;
                }
                return super.keyPressed(input);
        }

        private Text getKeybindText() {
                if (this.config.toggleKeyBinding == InputUtil.UNKNOWN_KEY.getCode()) {
                        return Text.literal("Touche d'activation: AUCUNE");
                }
                String keyName = InputUtil.Type.KEYSYM.createFromCode(this.config.toggleKeyBinding).getLocalizedText()
                                .getString();
                return Text.literal("Touche d'activation: " + keyName);
        }

        @Override
        public void close() {
                PasunhackConfig.save();
                this.client.setScreen(this.parent);
        }

        private void updateSearchResults(String query) {
                for (SearchResult res : this.searchResults) {
                        this.remove(res.button);
                }
                this.searchResults.clear();

                for (ButtonWidget btn : this.removeButtons) {
                        this.remove(btn);
                }
                this.removeButtons.clear();

                int centerX = this.width / 2;

                if (query.isEmpty()) {
                        int listY = 110;
                        int index = 0;
                        for (String block : this.config.blocksToMine) {
                                final String b = block;
                                ButtonWidget removeBtn = ButtonWidget
                                                .builder(Text.literal("§c[X] §r" + block), button -> {
                                                        this.config.blocksToMine.remove(b);
                                                        this.init();
                                                }).dimensions(centerX - 100, listY + (index * 24), 200, 20).build();
                                this.addDrawableChild(removeBtn);
                                this.removeButtons.add(removeBtn);
                                index++;
                        }
                } else {
                        String lowerQuery = query.toLowerCase();
                        List<Block> matches = Registries.BLOCK.stream()
                                        .filter(b -> {
                                                Identifier id = Registries.BLOCK.getId(b);
                                                return id.getPath().contains(lowerQuery)
                                                                || id.toString().contains(lowerQuery)
                                                                || b.getName().getString().toLowerCase()
                                                                                .contains(lowerQuery);
                                        })
                                        .limit(5)
                                        .collect(Collectors.toList());

                        int btnY = 110;
                        for (Block block : matches) {
                                String name = block.getName().getString();
                                Identifier id = Registries.BLOCK.getId(block);

                                ButtonWidget btn = ButtonWidget
                                                .builder(Text.literal("    " + name + " (" + id.getPath() + ")"),
                                                                button -> {
                                                                        String fullId = id.toString();
                                                                        if (!this.config.blocksToMine
                                                                                        .contains(fullId)) {
                                                                                this.config.blocksToMine.add(fullId);
                                                                                this.blockInputField.setText("");
                                                                                this.init();
                                                                        }
                                                                })
                                                .dimensions(centerX - 100, btnY, 200, 20).build();

                                this.addDrawableChild(btn);
                                this.searchResults.add(new SearchResult(block, btn));
                                btnY += 24;
                        }
                }
        }

        private void dumpTablistLogs() {
                if (this.client == null || this.client.player == null)
                        return;

                String basePath = "C:\\Users\\Omain\\Desktop\\meincrac\\";

                try (java.io.FileWriter writer = new java.io.FileWriter(basePath + "log1.txt")) {
                        writer.write("--- LOG 1 : PlayerListNetwork ---\n");
                        for (net.minecraft.client.network.PlayerListEntry entry : this.client.player.networkHandler
                                        .getPlayerList()) {
                                String name = entry.getProfile() != null ? entry.getProfile().name() : "null";
                                net.minecraft.text.Text displayNameObj = entry.getDisplayName();
                                String displayName = displayNameObj != null ? displayNameObj.getString() : "null";

                                net.minecraft.scoreboard.Team team = entry.getScoreboardTeam();
                                String teamName = team != null ? team.getName() : "null";
                                String prefix = team != null ? team.getPrefix().getString() : "";
                                String suffix = team != null ? team.getSuffix().getString() : "";

                                writer.write("ProfileName: " + name + " | DisplayName: " + displayName + " | TeamName: "
                                                + teamName + " | Prefix: " + prefix + " | Suffix: " + suffix + "\n");
                        }
                } catch (Exception e) {
                }

                try (java.io.FileWriter writer = new java.io.FileWriter(basePath + "log2.txt")) {
                        writer.write("--- LOG 2 : Scoreboard Teams ---\n");
                        net.minecraft.scoreboard.Scoreboard scoreboard = this.client.world.getScoreboard();
                        if (scoreboard != null) {
                                for (net.minecraft.scoreboard.Team team : scoreboard.getTeams()) {
                                        String prefix = team.getPrefix().getString();
                                        String suffix = team.getSuffix().getString();
                                        writer.write("Team: " + team.getName() + " | Prefix: " + prefix + " | Suffix: "
                                                        + suffix + " | Assigned Players: " + team.getPlayerList()
                                                        + "\n");
                                }
                        }
                } catch (Exception e) {
                }

                try (java.io.FileWriter writer = new java.io.FileWriter(basePath + "log3.txt")) {
                        writer.write("--- LOG 3 : Scoreboard Sidebar Objectives & All Objectives ---\n");
                        net.minecraft.scoreboard.Scoreboard scoreboard = this.client.world.getScoreboard();
                        if (scoreboard != null) {
                                net.minecraft.scoreboard.ScoreboardObjective objective = scoreboard.getObjectiveForSlot(
                                                net.minecraft.scoreboard.ScoreboardDisplaySlot.SIDEBAR);
                                if (objective != null) {
                                        writer.write("Sidebar Objective: " + objective.getName() + "\n");
                                        for (net.minecraft.scoreboard.ScoreboardEntry entry : scoreboard
                                                        .getScoreboardEntries(objective)) {
                                                String owner = entry.owner();
                                                net.minecraft.scoreboard.Team team = scoreboard
                                                                .getScoreHolderTeam(owner);
                                                String prefix = team != null ? team.getPrefix().getString() : "";
                                                String suffix = team != null ? team.getSuffix().getString() : "";
                                                writer.write("  Owner: " + owner + " | Score=" + entry.value()
                                                                + " | Prefix=" + prefix + " | Suffix=" + suffix + "\n");
                                        }
                                } else {
                                        writer.write("NO SIDEBAR OBJECTIVE FOUND\n");
                                }

                                writer.write("\n--- ALL OBJECTIVES ---\n");
                                for (net.minecraft.scoreboard.ScoreboardObjective obj : scoreboard.getObjectives()) {
                                        writer.write("Obj: " + obj.getName() + "\n");
                                        for (net.minecraft.scoreboard.ScoreboardEntry entry : scoreboard
                                                        .getScoreboardEntries(obj)) {
                                                writer.write("  Owner: " + entry.owner() + " = " + entry.value()
                                                                + "\n");
                                        }
                                }
                        }
                } catch (Exception e) {
                }
        }
}
