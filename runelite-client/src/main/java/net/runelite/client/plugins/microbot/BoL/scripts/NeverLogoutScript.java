package net.runelite.client.plugins.microbot.BoL.scripts;

import net.runelite.api.events.GameTick;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

import java.awt.event.KeyEvent;

public class NeverLogoutScript {
    private static final long randomDelay = Rs2Random.between(3000,5000);

    public static void onGameTick(GameTick event) {
        if (Rs2Player.checkIdleLogout(randomDelay)) {
            Microbot.log("pressing spacee");
            Rs2Keyboard.keyPress(KeyEvent.VK_BACK_SPACE);
        }
    }
}
