package net.rominux.pasunhack;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class PasunhackGui extends Screen {

        private final Screen parent;
        private final PasunhackConfig config;

        private TextFieldWidget blockInputField;
        private ButtonWidget toggleKeyButton;

        private boolean isListeningForKey = false;

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

                // Block Input Field
                this.blockInputField = new TextFieldWidget(this.textRenderer, centerX - 100, 80, 140, 20,
                                Text.literal("ID du bloc"));
                this.blockInputField.setMaxLength(100);
                this.addDrawableChild(this.blockInputField);

                // Add Block Button
                ButtonWidget addBlockButton = ButtonWidget.builder(Text.literal("Ajouter"), button -> {
                        String blockId = this.blockInputField.getText();
                        if (!blockId.isEmpty() && !this.config.blocksToMine.contains(blockId)) {
                                this.config.blocksToMine.add(blockId);
                                this.blockInputField.setText("");
                                this.init(); // Refresh UI to show the new block
                        }
                }).dimensions(centerX + 45, 80, 55, 20).build();
                this.addDrawableChild(addBlockButton);

                // List of blocks to remove
                int listY = 110;
                int index = 0;
                for (String block : this.config.blocksToMine) {
                        final String b = block;
                        ButtonWidget removeBtn = ButtonWidget.builder(Text.literal("§c[X] §r" + block), button -> {
                                this.config.blocksToMine.remove(b);
                                this.init(); // Refresh UI
                        }).dimensions(centerX - 100, listY + (index * 24), 200, 20).build();
                        this.addDrawableChild(removeBtn);
                        index++;
                }

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
}
