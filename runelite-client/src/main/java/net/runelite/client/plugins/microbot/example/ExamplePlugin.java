package net.runelite.client.plugins.microbot.example;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

import javax.inject.Inject;
import java.awt.*;

@PluginDescriptor(
        name = PluginDescriptor.Default + "Example",
        description = "Microbot example plugin",
        tags = {"example", "microbot"},
        enabledByDefault = false
)
@Slf4j
public class ExamplePlugin extends Plugin {
    @Inject
    private ExampleConfig config;
    @Provides
    ExampleConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(ExampleConfig.class);
    }

    @Inject
    private OverlayManager overlayManager;
    @Inject
    private ExampleOverlay exampleOverlay;

    @Inject
    ExampleScript exampleScript;

    public static boolean attackDelay = false;
    public static int tickCount = 0;


    @Override
    protected void startUp() throws AWTException {
        if (overlayManager != null) {
            overlayManager.add(exampleOverlay);
        }
        exampleScript.run(config);
    }

    // ✅ Method to adjust offsets from anywhere in your plugin
    public void adjustOffsets(int dx, int dy) {
        exampleOverlay.adjustOffset(dx, dy);
    }

    protected void shutDown() {
        exampleScript.shutdown();
        overlayManager.remove(exampleOverlay);
    }

    int ticks = 10;
    @Subscribe
    public void onGameTick(GameTick tick)
    {

        if (attackDelay) {
            tickCount++;

            if (tickCount >= 3) {
                attackDelay = false; // Reset the boolean after 3 ticks
                tickCount = 0; // Reset the counter
                Microbot.log("3 ticks elapsed. eating attack delay over.");
                // Set your script's boolean or trigger action here
            }
        }

        if (ticks > 0) {
            ticks--;
        } else {
            ticks = 10;
        }

    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {
        if (event.getMenuOption().equalsIgnoreCase("Cast") &&
                (event.getMenuTarget().contains("High Level Alchemy"))) {

            String itemName = extractItemName(event.getMenuTarget());
            int profit = getAlchProfitByName(itemName);

            if (profit > 0) {
                Microbot.log("profit " + profit);
            }
        }
            Microbot.log(
                    "[MENU] option=" + event.getMenuOption() +
                            " | type=" + event.getMenuAction() +
                            " | param0=" + event.getParam0() +
                            " | param1=" + event.getParam1() +
                            " | target=" + Text.removeTags(event.getMenuTarget())
            );
        }

    private String extractItemName(String menuTarget) {
        String clean = menuTarget.replaceAll("<[^>]*>", "");
        int idx = clean.indexOf("->");
        return idx != -1 ? clean.substring(idx + 2).trim() : clean.trim();
    }

    /**
     * Lookup by item *name*, no ID conversion needed.
     */
    private int getAlchProfitByName(String itemName) {
        switch (itemName.toLowerCase()) {
            case "steel platebody":
                return 250;
            case "rune axe":
            case "rune med helm":
                return 300;
            case "rune pickaxe":
            case "rune mace":
                return 450;
            case "rune platebody":
                return 600;
            default:
                return 0;
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage event) {
        String msg = event.getMessage();

        Microbot.log("Received message: " + event.getMessage() + " | Type: " + event.getType());

        if (msg.contains("you catch a raw herring.")) {
            Microbot.log("herring");
        }

        if (event.getType() == ChatMessageType.SPAM) {

        if (event.getMessage().contains("You hammer the bronze and make a longsword.")) {
            Microbot.log("3.1");
        }

            // Fixed: use toLowerCase() for case-insensitive matching
            if (msg.toLowerCase().contains("you catch a raw herring.")) {
               Microbot.log("herring fixed");
            }
        }

        if (msg.equals("You eat half the apple pie.") ||
                msg.equals("You eat the remaining apple pie.")) {
            attackDelay = true;
            tickCount = 0; // Reset the tick counter
        }

            if (event.getType() == ChatMessageType.GAMEMESSAGE) {
                String message = Text.removeTags(event.getMessage()).toLowerCase();
                if (message.contains("you can't light a fire here.")) {
                    Microbot.log("cannot light fire registered in plugin");
                }

                if (message.contains("you catch a raw shrimp.")) {
                    Microbot.log("shrimp1");
                }

                if (message.equals("you need an axe to chop down this tree.") || message.equals("you do not have an axe which you have the woodcutting level to use.")){
                    Microbot.log("you need an axe message registered");
                }

                //Microbot.log("Message received: '" + event.getMessage() + "'");
                if (message.contains("oh dear, you are dead!")) {

                    Microbot.log("1");
                }
                //Microbot.log("Message received: '" + event.getMessage() + "'");
                if (message.contains("you catch a raw herring.")) {
                    Microbot.log("2");

                }
                //Microbot.log("Message received: '" + event.getMessage() + "'");
                if (message.contains("you hammer the bronze and make a longsword.")) {
                    Microbot.log("3");

                }
            }

        //Microbot.log("Message received: '" + event.getMessage() + "'");
        if (event.getMessage().toLowerCase().contains("is already in your clan.")) {
            Microbot.log("registered already in clan message");
        }
    }

        }
