package net.rominux.pasunhack;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatSolverUtils {

    private static final Map<String, String> FETCHUR_ANSWERS;
    private static final Pattern FETCHUR_PATTERN = Pattern
            .compile("^\\[NPC\\] Fetchur: (?:its|theyre) ([a-zA-Z, \\-]*)$");
    private static final Pattern PUZZLER_PATTERN = Pattern.compile("^\\[NPC\\] Puzzler: ((?:▲|▶|◀|▼){10})$");

    static {
        FETCHUR_ANSWERS = new HashMap<>();
        FETCHUR_ANSWERS.put("yellow and see through", "Yellow Stained Glass");
        FETCHUR_ANSWERS.put("circular and sometimes moves", "Compass");
        FETCHUR_ANSWERS.put("expensive minerals", "Mithril");
        FETCHUR_ANSWERS.put("useful during celebrations", "Firework Rocket");
        FETCHUR_ANSWERS.put("hot and gives energy", "Cheap / Decent / Black Coffee");
        FETCHUR_ANSWERS.put("tall and can be opened", "Any Wooden Door / Iron Door");
        FETCHUR_ANSWERS.put("brown and fluffy", "Rabbit's Foot");
        FETCHUR_ANSWERS.put("explosive but more than usual", "Superboom TNT");
        FETCHUR_ANSWERS.put("wearable and grows", "Pumpkin");
        FETCHUR_ANSWERS.put("shiny and makes sparks", "Flint and Steel");
        FETCHUR_ANSWERS.put("green and some dudes trade stuff for it", "Emerald");
        FETCHUR_ANSWERS.put("red and soft", "Red Wool");
    }

    public static void init() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay)
                return;

            String text = message.getString();

            // Check for Fetchur
            if (PasunhackConfig.getInstance().solveFetchur) {
                Matcher fetchurMatcher = FETCHUR_PATTERN.matcher(text);
                if (fetchurMatcher.matches()) {
                    String riddle = fetchurMatcher.group(1);
                    String answer = FETCHUR_ANSWERS.getOrDefault(riddle, riddle);
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.player != null) {
                        client.player.sendMessage(Text.literal("§e[NPC] Fetchur§f: " + answer), false);
                    }
                }
            }

            // Check for Puzzler
            if (PasunhackConfig.getInstance().solvePuzzler) {
                Matcher puzzlerMatcher = PUZZLER_PATTERN.matcher(text);
                if (puzzlerMatcher.matches()) {
                    int x = 181;
                    int z = 135;
                    for (char c : puzzlerMatcher.group(1).toCharArray()) {
                        if (c == '▲')
                            z++;
                        else if (c == '▼')
                            z--;
                        else if (c == '◀')
                            x++;
                        else if (c == '▶')
                            x--;
                    }
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.world != null) {
                        BlockPos targetPos = new BlockPos(x, 195, z);
                        client.world.setBlockState(targetPos, Blocks.CRIMSON_PLANKS.getDefaultState());
                    }
                }
            }
        });
    }
}
