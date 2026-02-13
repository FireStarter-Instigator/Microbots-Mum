package net.runelite.client.plugins.microbot.util.trade;

import net.runelite.api.MenuAction;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.menu.NewMenuEntry;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

import java.awt.*;
import java.util.Map;

import static net.runelite.client.plugins.microbot.util.Global.sleep;
import static net.runelite.client.plugins.microbot.util.Global.sleepUntil;

public class Rs2Trade {

    /**
     * Offer a single item with specified quantity
     * @param itemId The item ID to offer
     * @param quantity The quantity to offer
     * @return true if successful, false otherwise
     */
    public static boolean offer(int itemId, int quantity) {
        return offerSingleItem(itemId, quantity);
    }

    /**
     * Offer a single item certificate with specified quantity
     * Converts item ID to certificate by adding 1
     * @param itemId The item ID to convert to certificate
     * @param quantity The quantity to offer
     * @return true if successful, false otherwise
     */
    public static boolean offerCert(int itemId, int quantity) {
        int certId = itemId + 1; // Convert item ID to cert ID
        Microbot.log("DEBUG: Converting item ID " + itemId + " to cert ID " + certId);
        return offerSingleItem(certId, quantity);
    }

    /**
     * Offer multiple item certificates with the same quantity
     * Converts item IDs to certificates by adding 1 to each
     * @param quantity The quantity for all items
     * @param itemIds Variable number of item IDs
     * @return true if all items were offered successfully, false otherwise
     */
    public static boolean offerCert(int quantity, int... itemIds) {
        try {
            for (int i = 0; i < itemIds.length; i++) {
                int certId = itemIds[i] + 1;
                if (!offerSingleItem(certId, quantity)) {
                    return false;
                }

                // Add delay between items (except after the last one)
                if (i < itemIds.length - 1) {
                    sleep(500);
                }
            }
            return true;
        } catch (Exception e) {
            Microbot.logStackTrace("Rs2Trade.offerCert", e);
            return false;
        }
    }

    /**
     * Offer multiple items with the same quantity
     * @param quantity The quantity for all items
     * @param itemIds Variable number of item IDs
     * @return true if all items were offered successfully, false otherwise
     */
    public static boolean offer(int quantity, int... itemIds) {
        try {
            for (int i = 0; i < itemIds.length; i++) {
                if (!offerSingleItem(itemIds[i], quantity)) {
                    return false;
                }

                // Add delay between items (except after the last one)
                if (i < itemIds.length - 1) {
                    sleep(500);
                }
            }
            return true;
        } catch (Exception e) {
            Microbot.logStackTrace("Rs2Trade.offer", e);
            return false;
        }
    }

    /**
     * Offer items with individual quantities using a map
     * @param itemQuantities Map of itemId -> quantity
     * @return true if all items were offered successfully, false otherwise
     */
    public static boolean offer(Map<Integer, Integer> itemQuantities) {
        try {
            int itemCount = 0;
            for (Map.Entry<Integer, Integer> entry : itemQuantities.entrySet()) {
                if (!offerSingleItem(entry.getKey(), entry.getValue())) {
                    return false;
                }

                itemCount++;
                // Add delay between items (except after the last one)
                if (itemCount < itemQuantities.size()) {
                    sleep(500);
                }
            }
            return true;
        } catch (Exception e) {
            Microbot.logStackTrace("Rs2Trade.offer", e);
            return false;
        }
    }

    /**
     * Offer items using pairs of (itemId, quantity)
     * @param itemQuantityPairs Variable number of arguments in pairs: itemId1, qty1, itemId2, qty2, etc.
     * @return true if all items were offered successfully, false otherwise
     */
    public static boolean offerPairs(int... itemQuantityPairs) {
        if (itemQuantityPairs.length % 2 != 0) {
            Microbot.log("Error: offerPairs requires an even number of arguments (itemId, quantity pairs)");
            return false;
        }

        try {
            for (int i = 0; i < itemQuantityPairs.length; i += 2) {
                int itemId = itemQuantityPairs[i];
                int quantity = itemQuantityPairs[i + 1];

                if (!offerSingleItem(itemId, quantity)) {
                    return false;
                }

                // Add delay between items (except after the last pair)
                if (i < itemQuantityPairs.length - 2) {
                    sleep(500);
                }
            }
            return true;
        } catch (Exception e) {
            Microbot.logStackTrace("Rs2Trade.offerPairs", e);
            return false;
        }
    }

    public static boolean offerCertPairs(Script script, int... itemQuantityPairs) {
        if (itemQuantityPairs.length % 2 != 0) {
            Microbot.log("Error: offerCertPairs requires an even number of arguments (itemId, quantity pairs)");
            return false;
        }

        try {
            for (int i = 0; i < itemQuantityPairs.length; i += 2) {
                if (!script.isRunning()) {
                    Microbot.log("Script stopped, aborting offerCertPairs");
                    return false;
                }

                int itemId = itemQuantityPairs[i];
                int certId = itemId + 1;
                int quantity = itemQuantityPairs[i + 1];

                if (!offerSingleItem(certId, quantity)) {
                    return false;
                }

                if (i < itemQuantityPairs.length - 2) {
                    sleep(500);
                }
            }
            return true;
        } catch (Exception e) {
            Microbot.logStackTrace("Rs2Trade.offerCertPairs", e);
            return false;
        }
    }

    /**
     * Offer certificate items with individual quantities using a map
     * Converts item IDs to certificates by adding 1 to each
     * @param itemQuantities Map of itemId -> quantity (itemIds will be converted to certs)
     * @return true if all items were offered successfully, false otherwise
     */
    public static boolean offerCert(Map<Integer, Integer> itemQuantities) {
        try {
            int itemCount = 0;
            for (Map.Entry<Integer, Integer> entry : itemQuantities.entrySet()) {
                int certId = entry.getKey() + 1; // Convert to cert
                Microbot.log("DEBUG: Converting item ID " + entry.getKey() + " to cert ID " + certId);

                if (!offerSingleItem(certId, entry.getValue())) {
                    return false;
                }

                itemCount++;
                // Add delay between items (except after the last one)
                if (itemCount < itemQuantities.size()) {
                    sleep(500);
                }
            }
            return true;
        } catch (Exception e) {
            Microbot.logStackTrace("Rs2Trade.offerCert", e);
            return false;
        }
    }

    public static boolean offerSingleItem(int itemId, int quantity) {
        return offerSingleItem(itemId, quantity, false);
    }

    public static boolean offerSingleItem(int itemId, int quantity, boolean slowMode) {

        Microbot.log("DEBUG: Attempting to offer item ID " + itemId + " with quantity " + quantity);

        Rs2ItemModel item = Rs2Inventory.items()
                .filter(it -> it != null && it.getId() == itemId)
                .findFirst()
                .orElse(null);

        if (item == null) {
            Microbot.log("DEBUG: Could not find item ID: " + itemId + " in inventory");
            return false;
        }

        int slotIndex = item.getSlot();
        String itemName = item.getName();

        Rectangle itemBounds = Rs2Inventory.itemBounds(item);
        if (itemBounds == null) {
            itemBounds = new Rectangle(1, 1);
        }

        Microbot.doInvoke(new NewMenuEntry(
                "Offer-X",
                slotIndex,
                22020096,
                MenuAction.CC_OP.getId(),
                5,
                itemId,
                itemName
        ), itemBounds);

        boolean dialogAppeared = sleepUntil(() ->
                Rs2Widget.isWidgetVisible(162, 32) ||
                        Rs2Widget.isWidgetVisible(548, 92) ||
                        Rs2Widget.isWidgetVisible(219, 1), 5000);

        if (!dialogAppeared) {
            return false;
        }

        // --- Adjustable sleep times ---
        int sleep1 = slowMode ? 1800 : 900;
        int sleep2 = slowMode ? 1200 : 800;

        sleep(sleep1);
        Rs2Keyboard.typeString(String.valueOf(quantity));
        sleep(sleep2);
        Rs2Keyboard.enter();

        //sleepUntil(() ->
                //!Rs2Widget.isWidgetVisible(162, 32) &&
                        //!Rs2Widget.isWidgetVisible(548, 92) &&
                        //!Rs2Widget.isWidgetVisible(219, 1), 3000);

        sleep(600);

        Microbot.log("DEBUG: Successfully completed offering " + itemName);
        return true;
    }

    /**
     * Checks safely if the other player has accepted the trade.
     */
    public static boolean otherPlayerAccepted() {
        final boolean[] result = {false};
        final boolean[] completed = {false};

        Microbot.getClientThread().invoke(() -> {
            try {
                Widget widget = Microbot.getClient().getWidget(InterfaceID.TRADEMAIN, 30);
                if (widget != null && !widget.isHidden()) {
                    String text = widget.getText();
                    //Microbot.log("Widget text: " + text);
                    result[0] = text != null && text.contains("Other player has accepted");
                } else {
                    Microbot.log("Widget of other accepted trade is null or hidden");
                }
            } finally {
                synchronized (completed) {
                    completed[0] = true;
                    completed.notify();
                }
            }
        });

        // Wait for completion
        synchronized (completed) {
            while (!completed[0]) {
                try {
                    completed.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }

        return result[0];
    }

    /**
     * Checks if we are currently in the first trade window.
     * Uses the same widget check as waitAndFinalizeTrade.
     * @return true if in first trade window, false otherwise
     */
    public static boolean isInFirstTradeWindow() {
        try {
            return Rs2Widget.getWidget(21954561) != null;
        } catch (Exception e) {
            Microbot.logStackTrace("Rs2Trade.isInFirstTradeWindow", e);
            return false;
        }
    }

    /**
     * Waits for the other player to accept, then finalizes the trade.
     * @return true if trade was successfully completed, false if any step failed
     */
    public static boolean waitAndFinalizeTrade() {
        Microbot.log("Waiting for trade window...");

        if (!sleepUntil(() -> Rs2Widget.getWidget(21954561) != null, 30000)) {
            Microbot.log("No trade window found - aborting.");
            return false;
        }

        Microbot.log("Trade window found! Waiting for other player to accept...");

        boolean accepted = sleepUntil(Rs2Trade::otherPlayerAccepted, 300000);
        if (!accepted) {
            Microbot.log("Timed out after 5 minutes - other player didn't accept.");
            return false;
        }

        Microbot.log("Other player accepted - finalizing trade...");
        return finalizeTrade();
    }


    /**
     * Finalizes a trade by pressing both accept buttons
     * with a 30-second timeout for the other player.
     * @return true if trade was successfully finalized, false if any step failed
     */
    public static boolean finalizeTrade() {
        // First accept
        Rs2Widget.clickWidget(21954570);
        Microbot.log("Clicked first accept");

        // Wait until first accept widget disappears
        boolean firstAcceptSucceeded = sleepUntil(() -> Rs2Widget.getWidget(21954570) == null, 30000);
        if (!firstAcceptSucceeded) {
            Microbot.log("First accept widget never disappeared - trade failed");
            return false;
        }

        sleep(1250);

        // Second accept
        Rs2Widget.clickWidget(21889049);
        Microbot.log("Clicked second accept");

        // Wait until second accept widget disappears
        boolean secondAcceptSucceeded = sleepUntil(() -> Rs2Widget.getWidget(21889049) == null, 30000);
        if (!secondAcceptSucceeded) {
            Microbot.log("Second accept widget never disappeared - trade failed");
            return false;
        }

        sleep(2000);
        Microbot.log("Trade successfully finalized");
        return true;
    }
}