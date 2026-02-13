package net.runelite.client.plugins.microbot.util.grandexchange.misc;

import net.runelite.api.gameval.ItemID;
import net.runelite.client.game.ItemManager;

public class PriceHelper {

    private static ItemManager itemManager;

    public static void init(ItemManager manager) {
        itemManager = manager;
    }

    public static int getPrice(int itemId) {
        if (itemManager == null) return -1;
        return itemManager.getItemPrice(itemId);
    }

    public static int getBronzeLongswordLoss() {
        int bar = getPrice(ItemID.BRONZE_BAR);

        if (bar <= 0) {
            return 350; // fallback to old static value
        }

        int cost = bar * 2;
        int value = 110; // your static longsword value

        return cost - value; // net loss
    }
}
