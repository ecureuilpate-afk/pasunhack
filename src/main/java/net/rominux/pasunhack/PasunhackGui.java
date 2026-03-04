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
        private double scrollOffset = 0;

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

                this.addDrawableChild(new net.minecraft.client.gui.widget.SliderWidget(
                                rX, 215, 130, 20, Text.literal("Tracer Radius: " + config.titaniumTracerRadius),
                                config.titaniumTracerRadius / 100.0) {
                        @Override
                        protected void updateMessage() {
                                this.setMessage(Text.literal("Tracer Radius: " + config.titaniumTracerRadius));
                        }

                        @Override
                        protected void applyValue() {
                                config.titaniumTracerRadius = (int) (this.value * 100);
                        }
                });

                this.addDrawableChild(ButtonWidget
                                .builder(Text.literal("Dump Full Logs"),
                                                btn -> {
                                                        dumpLogs();
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

                context.enableScissor(0, 105, this.width, this.height - 40);

                for (SearchResult res : this.searchResults) {
                        res.button.setY(110 + (int) this.scrollOffset + (this.searchResults.indexOf(res) * 26));
                        res.button.render(context, mouseX, mouseY, delta);
                        int x = res.button.getX() + 4;
                        int y = res.button.getY() + 3;
                        context.drawItem(new ItemStack(res.block), x, y);
                }

                if (this.blockInputField.getText().isEmpty()) {
                        int centerX = this.width / 2;
                        int listY = 110;
                        int index = 0;
                        for (int i = 0; i < this.removeButtons.size(); i += 3) {
                                int btnY = listY + (index * 26);
                                ButtonWidget rem = this.removeButtons.get(i);
                                ButtonWidget up = this.removeButtons.get(i + 1);
                                ButtonWidget down = this.removeButtons.get(i + 2);
                                rem.setY(btnY);
                                up.setY(btnY);
                                down.setY(btnY);

                                int mouseScrolledY = mouseY + (int) this.scrollOffset;
                                rem.render(context, mouseX, mouseScrolledY, delta);
                                up.render(context, mouseX, mouseScrolledY, delta);
                                down.render(context, mouseX, mouseScrolledY, delta);

                                if (index < this.config.blocksToMine.size()) {
                                        try {
                                                Identifier id = Identifier.of(this.config.blocksToMine.get(index));
                                                if (Registries.BLOCK.containsId(id)) {
                                                        Block b = Registries.BLOCK.get(id);
                                                        int x = centerX - 100 + 4;
                                                        int drawY = btnY + 2;
                                                        context.drawItem(new ItemStack(b), x, drawY);
                                                }
                                        } catch (Exception e) {
                                        }
                                }
                                index++;
                        }
                }

                context.disableScissor();
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
                this.scrollOffset += verticalAmount * -26;
                if (this.scrollOffset < 0)
                        this.scrollOffset = 0;
                return true;
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
                        int listY = 110 + (int) this.scrollOffset;
                        int index = 0;
                        for (String block : this.config.blocksToMine) {
                                final String b = block;
                                final int i = index;
                                int btnY = listY + (index * 26);
                                ButtonWidget removeBtn = ButtonWidget
                                                .builder(Text.literal("\u00A7c[X] \u00A7r" + block), button -> {
                                                        this.config.blocksToMine.remove(b);
                                                        this.init();
                                                }).dimensions(centerX - 100, btnY, 150, 24).build();

                                ButtonWidget upBtn = ButtonWidget
                                                .builder(Text.literal("\u2191"), button -> {
                                                        if (i > 0) {
                                                                java.util.Collections.swap(this.config.blocksToMine, i,
                                                                                i - 1);
                                                                this.init();
                                                        }
                                                }).dimensions(centerX + 55, btnY, 20, 24).build();

                                ButtonWidget downBtn = ButtonWidget
                                                .builder(Text.literal("\u2193"), button -> {
                                                        if (i < this.config.blocksToMine.size() - 1) {
                                                                java.util.Collections.swap(this.config.blocksToMine, i,
                                                                                i + 1);
                                                                this.init();
                                                        }
                                                }).dimensions(centerX + 80, btnY, 20, 24).build();

                                this.addSelectableChild(removeBtn);
                                this.addSelectableChild(upBtn);
                                this.addSelectableChild(downBtn);

                                this.removeButtons.add(removeBtn);
                                this.removeButtons.add(upBtn);
                                this.removeButtons.add(downBtn);
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

                        int btnY = 110 + (int) this.scrollOffset;
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
                                                .dimensions(centerX - 100, btnY, 200, 24).build();

                                this.addSelectableChild(btn);
                                this.searchResults.add(new SearchResult(block, btn));
                                btnY += 26;
                        }
                }
        }

        private void dumpLogs() {
                if (this.client == null || this.client.player == null)
                        return;
                try {
                        String baseDir = "C:\\Users\\Omain\\Desktop\\meincrac\\pasunhack-template-1.21.10\\log\\";
                        java.nio.file.Files.createDirectories(java.nio.file.Paths.get(baseDir));

                        // Scoreboard.txt
                        try (java.io.FileWriter fw = new java.io.FileWriter(baseDir + "Scoreboard.txt")) {
                                net.minecraft.scoreboard.Scoreboard sb = this.client.world.getScoreboard();
                                net.minecraft.scoreboard.ScoreboardObjective obj = sb.getObjectiveForSlot(
                                                net.minecraft.scoreboard.ScoreboardDisplaySlot.SIDEBAR);
                                if (obj != null) {
                                        fw.write("TITRE: " + obj.getDisplayName().getString() + "\n\n");
                                        for (net.minecraft.scoreboard.ScoreboardEntry entry : sb
                                                        .getScoreboardEntries(obj)) {
                                                net.minecraft.scoreboard.Team t = sb.getScoreHolderTeam(entry.owner());
                                                String prefix = t != null ? t.getPrefix().getString() : "";
                                                String suffix = t != null ? t.getSuffix().getString() : "";
                                                String owner = entry.owner().replaceAll("\u00A7.", "");
                                                fw.write(prefix + owner + suffix + "\n");
                                        }
                                }
                        }

                        // Tab.txt
                        try (java.io.FileWriter fw = new java.io.FileWriter(baseDir + "Tab.txt")) {
                                for (net.minecraft.client.network.PlayerListEntry e : this.client.player.networkHandler
                                                .getPlayerList()) {
                                        String raw = e.getDisplayName() != null ? e.getDisplayName().getString()
                                                        : e.getProfile().name();
                                        fw.write("Raw/Clean: " + raw + "\n");
                                }
                        }

                        // ChatHistory.txt
                        try (java.io.FileWriter fw = new java.io.FileWriter(baseDir + "ChatHistory.txt")) {
                                try {
                                        // Read ChatHud message lines dynamically (received messages)
                                        java.util.List<?> lines = null;
                                        try {
                                                java.lang.reflect.Method m = this.client.inGameHud.getChatHud()
                                                                .getClass()
                                                                .getDeclaredMethod("getMessages");
                                                m.setAccessible(true);
                                                lines = (java.util.List<?>) m
                                                                .invoke(this.client.inGameHud.getChatHud());
                                        } catch (Exception e) {
                                                // Fallback to latest.log if reflection fails
                                                java.io.File logFile = new java.io.File(client.runDirectory,
                                                                "logs/latest.log");
                                                if (logFile.exists()) {
                                                        java.util.List<String> fileLines = java.nio.file.Files
                                                                        .readAllLines(logFile.toPath());
                                                        int start = Math.max(0, fileLines.size() - 500);
                                                        for (int i = start; i < fileLines.size(); i++) {
                                                                fw.write(fileLines.get(i) + "\n");
                                                        }
                                                }
                                                throw e; // To skip to the sent messages fallback if log reading somehow
                                                         // fails
                                        }

                                        if (lines != null) {
                                                for (Object msg : lines) {
                                                        try {
                                                                java.lang.reflect.Method contentM = msg.getClass()
                                                                                .getDeclaredMethod("content");
                                                                contentM.setAccessible(true);
                                                                net.minecraft.text.Text text = (net.minecraft.text.Text) contentM
                                                                                .invoke(msg);
                                                                fw.write(text.getString() + "\n");
                                                        } catch (Exception e) {
                                                                // Not all objects might have content(), just ignore
                                                        }
                                                }
                                        }
                                } catch (Exception e) {
                                        fw.write("Fallback (sent messages):\n");
                                        for (String msg : this.client.inGameHud.getChatHud().getMessageHistory()) {
                                                fw.write(msg + "\n");
                                        }
                                }
                        }

                        // Entity.json
                        try (java.io.FileWriter fw = new java.io.FileWriter(baseDir + "Entity.json")) {
                                fw.write("[\n");
                                boolean first = true;
                                for (net.minecraft.entity.Entity e : this.client.world.getEntities()) {
                                        if (e.squaredDistanceTo(this.client.player) < 100 * 100) {
                                                if (!first)
                                                        fw.write(",\n");
                                                first = false;
                                                fw.write("  { \"type\": \"" + e.getType().getUntranslatedName()
                                                                + "\", \"uuid\": \"" + e.getUuidAsString()
                                                                + "\", \"name\": \""
                                                                + e.getName().getString().replace("\"", "\\\"")
                                                                + "\", \"x\": " + e.getX() + ", \"y\": " + e.getY()
                                                                + ", \"z\": " + e.getZ() + " }");
                                        }
                                }
                                fw.write("\n]\n");
                        }

                        // inventaire.json
                        try (java.io.FileWriter fw = new java.io.FileWriter(baseDir + "inventaire.json")) {
                                fw.write("[\n");
                                boolean first = true;
                                for (int i = 0; i < this.client.player.getInventory().size(); i++) {
                                        net.minecraft.item.ItemStack stack = this.client.player.getInventory()
                                                        .getStack(i);
                                        if (!stack.isEmpty()) {
                                                if (!first)
                                                        fw.write(",\n");
                                                first = false;
                                                String comps = stack.getComponents().toString().replace("\"", "\\\"");
                                                fw.write("  { \"slot\": " + i + ", \"item\": \""
                                                                + Registries.ITEM.getId(stack.getItem())
                                                                + "\", \"count\": " + stack.getCount()
                                                                + ", \"components\": \"" + comps + "\" }");
                                        }
                                }
                                fw.write("\n]\n");
                        }
                } catch (Exception e) {
                        e.printStackTrace();
                }
        }
}
