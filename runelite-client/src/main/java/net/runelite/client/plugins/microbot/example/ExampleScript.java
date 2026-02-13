package net.runelite.client.plugins.microbot.example;

import com.google.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.globval.GlobalWidgetInfo;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.combat.Rs2Combat;
import net.runelite.client.plugins.microbot.util.discord.Rs2Discord;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.menu.NewMenuEntry;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.player.Rs2PlayerModel;
import net.runelite.client.plugins.microbot.util.walker.Rs2MiniMap;
import net.runelite.client.ui.ClientUI;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.runelite.api.gameval.ItemID.BRONZE_BAR;
import static net.runelite.api.gameval.ItemID.MAPLE_SHORTBOW;
import static net.runelite.client.plugins.microbot.globval.GlobalWidgetInfo.*;


public class ExampleScript extends Script {

    @Inject
    ExamplePlugin plugin;

    public static boolean walkNearestMiniMapPoint(WorldPoint worldPoint, double zoomDistance) {
        if (Microbot.getClient().getMinimapZoom() != zoomDistance)
            Microbot.getClient().setMinimapZoom(zoomDistance);

        Point point = Rs2MiniMap.worldToMinimap(worldPoint);

        if (point == null) {
            // Try to find the nearest non-null minimap point
            for (int radius = 1; radius <= 10; radius++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dy = -radius; dy <= radius; dy++) {
                        if (Math.abs(dx) != radius && Math.abs(dy) != radius) continue; // Check perimeter only
                        WorldPoint nearby = new WorldPoint(worldPoint.getX() + dx, worldPoint.getY() + dy, worldPoint.getPlane());
                        Point p = Rs2MiniMap.worldToMinimap(nearby);
                        if (p != null && Rs2MiniMap.isPointInsideMinimap(p)) {
                            Microbot.log("Found nearby valid minimap point: " + nearby + " @ " + p);
                            Microbot.getMouse().click(p);
                            return true;
                        }
                    }
                }
            }
            Microbot.log("walkNearestMiniMapPoint: no valid minimap point found near " + worldPoint);
            return false;
        }

        if (!Rs2MiniMap.isPointInsideMinimap(point)) {
            Microbot.log("walkNearestMiniMapPoint: point outside minimap bounds for " + worldPoint + " @ " + point);
            return false;
        }

        Microbot.getMouse().click(point);
        return true;
    }


    private void logLoginIndexIndefinitely() {
        while (!Thread.currentThread().isInterrupted()) {
            int idx = -1;
            try {
                if (Microbot.getClient() != null) {
                    idx = Microbot.getClient().getLoginIndex();
                }
            } catch (Exception e) {
                Microbot.log("Error reading loginIndex: " + e.getMessage());
            }

            Microbot.log("loginIndex = " + idx + " | time = " + System.currentTimeMillis());

            try {
                Thread.sleep(1000L);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                Microbot.log("logLoginIndexIndefinitely interrupted");
                break;
            }
        }
    }

    private static final int HIGH_ALCH_ANIMATION = 713;

    public static void estimateDistinctHighAlchers() {
        Set<String> seenNames = new HashSet<>();

        for (int i = 0; i < 3; i++) {
            Set<String> currentNames = Rs2Player.getPlayers(p -> p.getAnimation() == HIGH_ALCH_ANIMATION, false)
                    .map(Rs2PlayerModel::getName)
                    .filter(n -> n != null && !n.isEmpty())
                    .collect(Collectors.toSet());

            seenNames.addAll(currentNames);
            sleep(600); // wait between samples
        }

        Microbot.log("Estimated distinct High Alchers nearby: " + seenNames.size());
        seenNames.forEach(n -> Microbot.log(" - " + n));
    }

    public static boolean debugNearestMiniMapPointAroundPlayer(int maxRadius) {
        if (Microbot.getClient().getLocalPlayer() == null) {
            Microbot.log("Player not found.");
            return false;
        }

        WorldPoint playerLoc = Rs2Player.getWorldLocation();
        Microbot.log("Scanning minimap validity around player: " + playerLoc);

        for (int radius = 0; radius <= maxRadius; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    WorldPoint testPoint = new WorldPoint(
                            playerLoc.getX() + dx,
                            playerLoc.getY() + dy,
                            playerLoc.getPlane()
                    );

                    Point minimapPoint = Rs2MiniMap.worldToMinimap(testPoint);
                    if (minimapPoint != null && Rs2MiniMap.isPointInsideMinimap(minimapPoint)) {
                        Microbot.log("Valid minimap projection found:");
                        Microbot.log("  WorldPoint: " + testPoint);
                        Microbot.log("  ScreenPoint: " + minimapPoint);
                        Microbot.log("  Offset from player: dx=" + dx + ", dy=" + dy);
                        Microbot.getMouse().click(minimapPoint);
                        return true;
                    }
                }
            }
        }

        Microbot.log("No valid minimap projection found within " + maxRadius + " tiles of player.");
        return false;
    }
    @Inject
    Client client;


    public Boolean getAttackStyle() {
        // Check current attack style
        int attackStyle = Microbot.getClient().getVarpValue(VarPlayerID.COM_MODE);

        if (attackStyle == 4) {
            return true; // Return true if attackStyle is exactly 4
        } else if (attackStyle < 4) {
            return false; // Return false if attackStyle is less than 4
        }

        return null; // Optional: Return null for unexpected cases
    }

    public boolean isAttackStyleStrength() {
        int attackStyle = Microbot.getClient().getVarpValue(VarPlayerID.COM_MODE);
        return attackStyle == 1;
    }

    public boolean run(ExampleConfig config) {
        Microbot.enableAutoRunOn = false;

        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!super.run())
                    return;
                //debugNearestMiniMapPointAroundPlayer(10);
                //plugin.adjustOffsets(0, -5);               //ExampleOVerlay.adjustOffset(0, -5);   // move 5 pixels up
                //findAutoRetaliateWidget();
                //debugMouseClicks();
                //detectAutoRetaliateVarbit();

                int attackStyle = client.getVarpValue(VarPlayerID.COM_MODE);
                if (attackStyle == 3) {
                    Microbot.log("defensive?");
                }

                Rs2Combat.setAttackStyle(COMBAT_STYLE_TWO);
                Microbot.log("just set to combat style two");
                sleep(10000);
                Rs2Combat.setAttackStyle(COMBAT_STYLE_FOUR);
                Microbot.log("just set to combat style FOUR");

                if (Microbot.isLoggedIn()) {
                    sleep(60000);
                    return;
                }
                Microbot.log("attack style is 4" + getAttackStyle());
                if (getAttackStyle()) {

                }

                Microbot.log("is attack style strength" + isAttackStyleStrength());

                if (!isAttackStyleStrength()) {
                    Microbot.log("attack style is not strength");
                    Rs2Combat.setAttackStyle(COMBAT_STYLE_TWO);
                }


                Microbot.log("what is attack style" + Microbot.getClient().getVarpValue(VarPlayerID.COM_MODE));

                //Rs2Combat.setAttackStyle(COMBAT_STYLE_TWO);

                if (Microbot.isLoggedIn()) {
                    return;
                }

                long loopStart = System.currentTimeMillis();
                Microbot.log("========================================");
                Microbot.log("LOOP START: " + loopStart);
                Microbot.log("========================================");

                testCombatStyleChange();

                long loopEnd = System.currentTimeMillis();
                Microbot.log("========================================");
                Microbot.log("LOOP END: Duration = " + (loopEnd - loopStart) + "ms");
                Microbot.log("========================================");

                sleep(600000);



                    try {
                        String clientVersion = ClientUI.getFrame().getTitle();

                        String msg = "🚫 Banned/disabled account detected — index="
                                + Microbot.activeAccountIndex
                                + " — version=" + clientVersion;

                        boolean sent = Rs2Discord.sendWebhookMessage(msg);
                        if (!sent) {
                            Microbot.log("Discord webhook send failed for index " + Microbot.activeAccountIndex);
                        }
                    } catch (Exception e) {
                        Microbot.log("Error sending banned-account webhook: " + e.getMessage());
                    }

                //Rs2Combat.setAutoRetaliate(false);
                sleep(5000);
                if (Microbot.isLoggedIn()) {
                    sleep(60000);
                    return;
                }


                WorldPoint[] path = {
                        new WorldPoint(3165, 3479, 0),
                        new WorldPoint(3165, 3470, 0),
                        new WorldPoint(3165, 3459, 0),
                        new WorldPoint(3176, 3456, 0),
                        new WorldPoint(3183, 3451, 0),
                        new WorldPoint(3182, 3440, 0)
                };

              Rs2Player.logout();

                if (Microbot.isLoggedIn()) {
                    sleep(60000);
                    return;
                }

                estimateDistinctHighAlchers();
                sleep(60000);
                Rs2Player.toggleRunEnergy(true);
                sleep(5000);
                Microbot.log("2nd call");
                Rs2Player.toggleRunEnergy(true);
                sleep(600000);

              if (Rs2Bank.count(BRONZE_BAR) < 420) {Microbot.log("less than 420 bars in bank");} else {Microbot.log("more than 420 bars in bank");}
              sleep(500000);

                logLoginIndexIndefinitely();



                WorldPoint playerLoc = Rs2Player.getWorldLocation();
                WorldPoint geLoc = new WorldPoint(3164, 3487, 0);

                int distance = playerLoc.distanceTo(geLoc);
                Microbot.log("Player: " + playerLoc + " | GE: " + geLoc + " | Distance: " + distance);

                if (distance > 10) {
                    Microbot.log("You're more than 10 tiles away from GE");
                } else {
                    Microbot.log("You're within 10 tiles of GE");
                    sleep(60000);
                    return;
                }





                // Log all inventory slots at the start
                Microbot.log("=== STARTING INVENTORY CHECK ===");
                for (int i = 0; i <= 27; i++) {  // Changed to 0-27
                    int slotId = Rs2Inventory.getIdForSlot(i);
                    if (slotId > 0) {
                        Microbot.log("Slot " + i + " contains item ID: " + slotId);
                    }
                }
                Microbot.log("=================================");

// First item
                Microbot.log(">>> Checking slot 0 for first item...");
                int id = Rs2Inventory.getIdForSlot(0);
                Microbot.log("Slot 0 item ID: " + id);
                if (id == 1148 || id == 1120 || id == 1360) {
                    Microbot.log("✓ Slot 0 matches target items (1148/1120/1360)");
                    Microbot.log("Starting first drag from slot 0 to slot 8");
                    if (dragInventoryItemWithRetry(0, 8, id, 5)) {  // Changed to slot 0
                        Microbot.log("✓ First drag completed successfully");
                    } else {
                        Microbot.log("✗ First drag failed after retries");
                    }
                } else {
                    Microbot.log("✗ Slot 0 does NOT match target items. Item ID was: " + id);
                }
                Microbot.log("Waiting 2-4 seconds before next check...");
                sleep(2000, 4000);

// Second item
                Microbot.log(">>> Checking slot 1 for second item...");
                int id1 = Rs2Inventory.getIdForSlot(1);
                Microbot.log("Slot 1 item ID: " + id1);
                if (id1 == 1148 || id1 == 1120 || id1 == 1360) {
                    Microbot.log("✓ Slot 1 matches target items (1148/1120/1360)");
                    Microbot.log("Starting second drag from slot 1 to slot 12");
                    if (dragInventoryItemWithRetry(1, 12, id1, 5)) {  // This one is correct
                        Microbot.log("✓ Second drag completed successfully");
                    } else {
                        Microbot.log("✗ Second drag failed after retries");
                    }
                } else {
                    Microbot.log("✗ Slot 1 does NOT match target items. Item ID was: " + id1);
                }
                Microbot.log("Waiting 2-4 seconds before next check...");
                sleep(2000, 4000);

// Third item
                Microbot.log(">>> Checking slot 2 for third item...");
                int id2 = Rs2Inventory.getIdForSlot(2);
                Microbot.log("Slot 2 item ID: " + id2);
                if (id2 == 1148 || id2 == 1120 || id2 == 1360) {
                    Microbot.log("✓ Slot 2 matches target items (1148/1120/1360)");
                    Microbot.log("Starting third drag from slot 2 to slot 16");
                    if (dragInventoryItemWithRetry(2, 16, id2, 5)) {  // This one is correct
                        Microbot.log("✓ Third drag completed successfully");
                    } else {
                        Microbot.log("✗ Third drag failed after retries");
                    }
                } else {
                    Microbot.log("✗ Slot 2 does NOT match target items. Item ID was: " + id2);
                }

                Microbot.log("=== All drag operations complete ===");
                sleep(50000);

            } catch (Exception ex) {
                System.out.println("Error in login monitor: " + ex.getMessage());
            }

           // Microbot.log("End of login check cycle");
        }, 0, 2000, TimeUnit.MILLISECONDS);

        return true;
    }

    public void testCombatStyleChange() {
        Microbot.log("Checking for Maple Shortbow");

        if (!Rs2Inventory.contains(MAPLE_SHORTBOW)) {
            Microbot.log("ERROR: No Maple Shortbow in inventory!");
            return;
        }

        Microbot.log("Wielding Maple Shortbow");
        Rs2Inventory.interact(MAPLE_SHORTBOW, "Wield");
        sleep(600);

        Microbot.log("Calling Rs2Combat.setAttackStyleVerbose");
        Rs2Combat.setAttackStyleVerbose(COMBAT_STYLE_FOUR);

        Microbot.log("Finished test");
    }

    /**
     * Prints coordinates when you click - run this and click on the retaliate button
     */
    public static void debugMouseClicks() {
        System.out.println("=== Mouse Click Debugger ===");
        System.out.println("Click anywhere and the coordinates will be printed...");

        Point lastClick = new Point(-1, -1);

        for (int i = 0; i < 50; i++) {
            if (Microbot.getClient().getMouseCanvasPosition() != null) {
                Point currentPos = Microbot.getClient().getMouseCanvasPosition();

                // Detect if mouse button is pressed (this is approximate)
                System.out.println("Current mouse: X=" + currentPos.getX() + ", Y=" + currentPos.getY());
            }

            sleep(500);
        }

        System.out.println("=== Debug Complete ===");
    }
    public static void findAutoRetaliateWidget() {
        Client client = Microbot.getClient();
        if (client == null) {
            System.out.println("Client is null");
            return;
        }

        System.out.println("Starting comprehensive widget search...");
        System.out.println("Game state: " + client.getGameState());

        // Known combat-related widget groups in RuneLite
        int[] combatGroups = {
                593,  // Combat Options
                116,  // Options/Settings
                109,  // Account Management (sometimes has settings)
                261,  // Settings side panel
                134,  // Music/Settings tab area
        };

        System.out.println("\n=== Checking known combat/settings groups ===");
        for (int group : combatGroups) {
            System.out.println("\n--- Checking group " + group + " ---");

            for (int child = 0; child < 300; child++) {
                Widget w = client.getWidget(group, child);
                if (w == null) continue;

                // Log EVERYTHING about this widget
                String text = w.getText();
                String name = w.getName();
                int type = w.getType();
                boolean hidden = w.isHidden();
                int itemId = w.getItemId();
                int spriteId = w.getSpriteId();

                // Show all non-hidden widgets
                if (!hidden) {
                    System.out.println(
                            "  [" + group + ":" + child + "] " +
                                    "type=" + type +
                                    " name='" + name + "'" +
                                    " text='" + text + "'" +
                                    " spriteId=" + spriteId +
                                    " itemId=" + itemId +
                                    " id=" + w.getId()
                    );
                }

                // Check children too
                Widget[] children = w.getChildren();
                if (children != null) {
                    for (int i = 0; i < children.length && i < 50; i++) {
                        Widget child_w = children[i];
                        if (child_w == null || child_w.isHidden()) continue;

                        System.out.println(
                                "    └─ child[" + i + "] " +
                                        "type=" + child_w.getType() +
                                        " text='" + child_w.getText() + "'" +
                                        " name='" + child_w.getName() + "'"
                        );
                    }
                }
            }
        }

        // Also do a broader search for specific keywords
        System.out.println("\n\n=== Searching ALL groups for combat keywords ===");
        for (int group = 0; group < 1000; group++) {
            for (int child = 0; child < 100; child++) {
                Widget w = client.getWidget(group, child);
                if (w == null) continue;

                String text = w.getText();
                String name = w.getName();

                if ((text != null && (
                        text.toLowerCase().contains("retal") ||
                                text.toLowerCase().contains("attack") ||
                                text.toLowerCase().contains("combat") ||
                                text.toLowerCase().contains("option"))) ||
                        (name != null && (
                                name.toLowerCase().contains("retal") ||
                                        name.toLowerCase().contains("combat") ||
                                        name.toLowerCase().contains("attack")))) {

                    System.out.println(
                            "KEYWORD MATCH → [" + group + ":" + child + "] " +
                                    "text='" + text + "' " +
                                    "name='" + name + "' " +
                                    "type=" + w.getType() + " " +
                                    "hidden=" + w.isHidden()
                    );
                }
            }
        }

        System.out.println("\n=== Search complete! ===");
    }

    // Call this from the client thread or a safe context where Microbot operations are valid
    public static void detectAutoRetaliateVarbit() {
        Client client = Microbot.getClient();
        if (client == null) {
            System.out.println("Client null");
            return;
        }

        Widget toggle = client.getWidget(388, 284); // same widget you used
        if (toggle == null) {
            System.out.println("Toggle widget NOT FOUND!");
            return;
        }

        int widgetId = toggle.getId();
        int groupId = widgetId >>> 16;
        int childId = widgetId & 0xFFFF;

        // Build menu entry as in RecruiterScript
        NewMenuEntry entry = new NewMenuEntry(
                "Auto Retaliate",
                "",
                childId,
                MenuAction.CC_OP,
                groupId,
                -1,
                false
        );

        // read varbits snapshot helper
        java.util.Map<Integer, Integer> snapshotBefore = readVarbitsSnapshot(client, 0, 20000); // adjust range if you want

        // perform the click (set target and invoke)
        Microbot.targetMenu = entry;
        java.awt.Rectangle bounds = toggle.getBounds();
        if (bounds == null) {
            var loc = toggle.getCanvasLocation();
            bounds = new java.awt.Rectangle(loc.getX(), loc.getY(), toggle.getWidth(), toggle.getHeight());
        }

        Microbot.doInvoke(entry, bounds);

        // small wait for the client to apply change
        try { Thread.sleep(400); } catch (InterruptedException ignored) {}

        java.util.Map<Integer, Integer> snapshotAfter = readVarbitsSnapshot(client, 0, 20000);

        // print diffs
        System.out.println("Varbits changed (before -> after):");
        for (int id = 0; id <= 20000; id++) {
            Integer b = snapshotBefore.get(id);
            Integer a = snapshotAfter.get(id);
            if (b == null || a == null) continue;
            if (!b.equals(a)) {
                System.out.println("varbit " + id + " : " + b + " -> " + a);
            }
        }
    }

    /** Helper: read getVarbitValue across a range into a map */
    private static java.util.Map<Integer, Integer> readVarbitsSnapshot(Client client, int start, int end) {
        java.util.Map<Integer, Integer> m = new java.util.HashMap<>();
        for (int i = start; i <= end; i++) {
            try {
                int val = client.getVarbitValue(i);
                m.put(i, val);
            } catch (Exception ex) {
                // some clients may throw for out-of-range varbits; ignore
            }
        }
        return m;
    }


    /**
     * Print all inventory slots and their contents (including slot 0)
     * @return true if inventory has items, false if empty
     */
    public static boolean printInventoryState() {
        Microbot.log("==========================================");
        Microbot.log("       CURRENT INVENTORY STATE");
        Microbot.log("==========================================");

        int itemCount = 0;

        // Check slots 0-28
        for (int i = 0; i <= 28; i++) {
            int slotId = Rs2Inventory.getIdForSlot(i);
            if (slotId > 0) {
                Microbot.log("Slot " + String.format("%2d", i) + " | Item ID: " + slotId);
                itemCount++;
            } else {
                Microbot.log("Slot " + String.format("%2d", i) + " | EMPTY");
            }
        }

        Microbot.log("------------------------------------------");
        Microbot.log("Total items: " + itemCount);
        Microbot.log("==========================================");

        return itemCount > 0;
    }
    /**
     * Drag an item from one inventory slot to another with retry logic
     * Verifies success by checking if the source slot has changed
     * @return true if successful, false if max retries exceeded
     */
    public static boolean dragInventoryItemWithRetry(int fromSlot, int toSlot, int itemId, int maxRetries) {
        int attempts = 0;

        Microbot.log("[RETRY] Starting drag operation: slot " + fromSlot + " -> slot " + toSlot + " for item " + itemId);

        while (attempts < maxRetries) {
            attempts++;
            Microbot.log("[RETRY] === Attempt " + attempts + "/" + maxRetries + " ===");

            // Check if source slot still has the item we want to drag
            int currentFromId = Rs2Inventory.getIdForSlot(fromSlot);
            Microbot.log("[RETRY] Current item in source slot " + fromSlot + ": " + currentFromId);

            if (currentFromId != itemId) {
                Microbot.log("[RETRY] ✗ Source slot " + fromSlot + " no longer contains item " + itemId + " (found " + currentFromId + ")");
                return false;
            }

            // Store the BEFORE state
            Microbot.log("[RETRY] BEFORE drag - Source slot " + fromSlot + " contains: " + currentFromId);

            // Perform the drag
            Microbot.log("[RETRY] Executing drag operation...");
            boolean dragResult = dragInventoryItem(fromSlot, toSlot);
            Microbot.log("[RETRY] Drag method returned: " + dragResult);

            // Wait for the drag to complete
            Microbot.log("[RETRY] Waiting for drag to complete...");
            sleep(600, 1200);

            // Verify the drag was successful by checking if SOURCE slot changed
            int fromSlotIdAfter = Rs2Inventory.getIdForSlot(fromSlot);
            Microbot.log("[RETRY] AFTER drag - Source slot " + fromSlot + " contains: " + fromSlotIdAfter);

            // Success if the source slot changed (either empty or different item)
            if (fromSlotIdAfter != currentFromId) {
                Microbot.log("[RETRY] ✓ SUCCESS! Source slot " + fromSlot + " changed from " + currentFromId + " to " + fromSlotIdAfter);
                return true;
            }

            Microbot.log("[RETRY] ✗ Drag verification failed. Source slot " + fromSlot + " still contains " + fromSlotIdAfter + " (expected change from " + currentFromId + ")");
            Microbot.log("[RETRY] Waiting before retry...");
            sleep(300, 600);
        }

        Microbot.log("[RETRY] ✗✗✗ FAILED after " + maxRetries + " attempts ✗✗✗");
        return false;
    }

    /**
     * Improved drag method with validation (0-indexed slots)
     */
    public static boolean dragInventoryItem(int fromSlot, int toSlot) {
        Microbot.log("[DRAG] Attempting drag from slot " + fromSlot + " to slot " + toSlot);

        Widget inventory = Microbot.getClient().getWidget(WidgetInfo.INVENTORY);
        if (inventory == null || inventory.getChildren() == null) {
            Microbot.log("[DRAG] ✗ Inventory widget not found or has no children");
            return false;
        }
        Microbot.log("[DRAG] ✓ Inventory widget found");

        Widget[] slots = inventory.getChildren();
        Microbot.log("[DRAG] Total inventory slots: " + slots.length);

        // Validate slots are in range 0-27
        if (fromSlot < 0 || fromSlot >= slots.length || toSlot < 0 || toSlot >= slots.length) {
            Microbot.log("[DRAG] ✗ Invalid slot numbers. From: " + fromSlot + ", To: " + toSlot + ", Max: " + (slots.length - 1));
            return false;
        }

        Widget from = slots[fromSlot];  // No need to subtract 1 anymore
        Widget to = slots[toSlot];      // No need to subtract 1 anymore

        if (from == null) {
            Microbot.log("[DRAG] ✗ Source slot widget is null");
            return false;
        }
        if (to == null) {
            Microbot.log("[DRAG] ✗ Destination slot widget is null");
            return false;
        }
        if (from.getItemId() <= 0) {
            Microbot.log("[DRAG] ✗ Source slot is empty (itemId: " + from.getItemId() + ")");
            return false;
        }

        Microbot.log("[DRAG] Source slot item ID: " + from.getItemId());

        Point start = from.getCanvasLocation();
        Point end = to.getCanvasLocation();

        if (start == null) {
            Microbot.log("[DRAG] ✗ Could not get canvas location for source slot");
            return false;
        }
        if (end == null) {
            Microbot.log("[DRAG] ✗ Could not get canvas location for destination slot");
            return false;
        }

        Microbot.log("[DRAG] Start point: (" + start.getX() + ", " + start.getY() + ")");
        Microbot.log("[DRAG] End point: (" + end.getX() + ", " + end.getY() + ")");

        Microbot.getMouse().drag(start, end);
        Microbot.log("[DRAG] ✓ Drag executed from slot " + fromSlot + " to slot " + toSlot);
        return true;
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }
}