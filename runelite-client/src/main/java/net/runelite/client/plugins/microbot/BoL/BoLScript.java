package net.runelite.client.plugins.microbot.BoL;

import net.runelite.api.coords.WorldArea;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.shortestpath.ShortestPathPlugin;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static net.runelite.api.gameval.ItemID.APPLE_PIE;
import static net.runelite.api.gameval.ItemID.HALF_AN_APPLE_PIE;
import static net.runelite.client.plugins.microbot.util.player.Rs2Player.eatAt;


public class BoLScript extends Script {
    private final WorldArea monkArea = new WorldArea(3040, 3475, 23, 36, 0);
    private long lastEatCheckTime = 0;
    private boolean waitingToRecheckHealth = false;
    @Inject
    private BoLConfig config;
    @Inject
    private BoLPlugin boLPlugin;

    public boolean run(BoLConfig boLConfig) {
        Microbot.enableAutoRunOn = true;

        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn()) return;
                if (!super.run()) return;
                if (!config.eatPie()) return;

                boolean shouldCheckHealth = false;

                if (Rs2Player.isMoving() && ShortestPathPlugin.pathfinder != null) {eatAt(70);
                    shouldCheckHealth = eatAt(70);}

                if (monkArea.contains(Rs2Player.getLocalPlayer().getWorldLocation())) {eatAt(70);
                    shouldCheckHealth = eatAt(70) || shouldCheckHealth;}

                if (shouldCheckHealth) {
                    lastEatCheckTime = System.currentTimeMillis();
                    waitingToRecheckHealth = true;
                }

                if (waitingToRecheckHealth && System.currentTimeMillis() - lastEatCheckTime >= 5000) {
                    if (Rs2Player.getHealthPercentage() < 70) {
                        useApplePieOrHalf();
                    }
                    waitingToRecheckHealth = false;
                }

                if (Rs2Inventory.contains(APPLE_PIE) || Rs2Inventory.contains(HALF_AN_APPLE_PIE)) {
                    eatAt(60, true);
                    sleep(600);
                }

            } catch (Exception ex) {
                System.out.println("Error en loop: " + ex.getMessage());
            }

        }, 0, 1000, TimeUnit.MILLISECONDS);

        return true;
    }

    private void useApplePieOrHalf() {
        Rs2ItemModel applePie = Rs2Inventory.get(APPLE_PIE);
        Rs2ItemModel halfOfApplePie = Rs2Inventory.get(HALF_AN_APPLE_PIE);

        Microbot.log("entering manual eat mode");
        if (applePie != null) {
            Rs2Inventory.hover1(applePie);
        } else if (halfOfApplePie != null) {
            Rs2Inventory.hover1(halfOfApplePie);
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }
}