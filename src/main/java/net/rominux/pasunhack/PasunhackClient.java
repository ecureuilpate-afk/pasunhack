package net.rominux.pasunhack;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class PasunhackClient implements ClientModInitializer {

    private static boolean wasTogglePressed = false;
    private static KeyBinding menuKeyBinding;

    @Override
    public void onInitializeClient() {
        // Charge la configuration dès le départ
        PasunhackConfig.load();

        // Utilisation de la catégorie officielle MISC (Divers) pour éviter l'erreur
        menuKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Ouvrir le Menu Pasunhack",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                KeyBinding.Category.MISC));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            // 1. Ouvre le GUI si on appuie sur la touche du menu (G)
            while (menuKeyBinding.wasPressed()) {
                client.setScreen(new PasunhackGui(client.currentScreen));
            }

            // 2. Vérification direct niveau GLFW pour le toggle du mod
            if (client.getWindow() != null) {
                long window = client.getWindow().getHandle();
                int toggleKey = PasunhackConfig.getInstance().toggleKeyBinding;

                boolean isTogglePressed = false;
                if (toggleKey != InputUtil.UNKNOWN_KEY.getCode()) {
                    isTogglePressed = GLFW.glfwGetKey(window, toggleKey) == GLFW.GLFW_PRESS;
                }

                if (isTogglePressed && !wasTogglePressed) {
                    AutoMiner.toggle();
                    String status = AutoMiner.isEnabled() ? "§aActivé" : "§cDésactivé";
                    if (client.player != null) {
                        client.player.sendMessage(Text.literal("AutoMiner: " + status), true);
                    }
                }
                wasTogglePressed = isTogglePressed;
            }

            // 3. Appel à la logique du minage
            AutoMiner.tick(client);
        });
    }
}
