package net.runelite.client.plugins.microbot.BoL;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.BoL.scripts.DismissNpcEvent;
import net.runelite.client.plugins.microbot.BoL.scripts.NeverLogoutScript;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;

import javax.inject.Inject;
import java.awt.*;

@PluginDescriptor(
        name = PluginDescriptor.Bee + "BoL",
        description = "Reduced Qol",
        tags = {"QoL", "Bee"},
        enabledByDefault = false
)
@Slf4j
public class BoLPlugin extends Plugin {

    @Inject
    private BoLConfig config;
    @Inject
    BoLScript boLScript;
    private DismissNpcEvent dismissNpcEvent;

    @Provides
    BoLConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(BoLConfig.class);
    }


    @Override
    protected void startUp() throws AWTException {
        boLScript.run(config);
    }

    protected void shutDown() {
        Microbot.getBlockingEventManager().remove(dismissNpcEvent);
        dismissNpcEvent = null;
        boLScript.shutdown();
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        if (!Microbot.isLoggedIn()) return;
        if (config.neverLogout()) {
            NeverLogoutScript.onGameTick(event);
        }
        if (config.dialogues()) {
            if (Rs2Dialogue.isInDialogue()) {
                Rs2Dialogue.clickContinue();
            }

            if (Rs2Dialogue.isInDialogue()) {
                Rs2Dialogue.handleQuestOptionDialogueSelection();
            }
        }
        // Handle Random Event Dismissal dynamically
        if (config.dismissRandom()) {
            // Turn ON if not already active
            if (dismissNpcEvent == null) {
                dismissNpcEvent = new DismissNpcEvent();
                Microbot.getBlockingEventManager().add(dismissNpcEvent);
            }
        } else {
            // Turn OFF if active
            if (dismissNpcEvent != null) {
                Microbot.getBlockingEventManager().remove(dismissNpcEvent);
                dismissNpcEvent = null;
            }
        }
    }
}
