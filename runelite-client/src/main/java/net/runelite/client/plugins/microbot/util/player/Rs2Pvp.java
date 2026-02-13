package net.runelite.client.plugins.microbot.util.player;

import net.runelite.api.InventoryID;
import net.runelite.api.Varbits;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.api.geometry.Cuboid;
// We keep the Microbot WorldPoint import for internal use, but we will fully qualify the API one in parameters
import net.runelite.client.plugins.microbot.util.api.coords.WorldPoint;
import net.runelite.client.util.QuantityFormatter;
import org.apache.commons.lang3.ArrayUtils;

import java.awt.*;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class Rs2Pvp {
    private static final Polygon NOT_WILDERNESS_BLACK_KNIGHTS = new Polygon( // this is black knights castle
            new int[]{2994, 2995, 2996, 2996, 2994, 2994, 2997, 2998, 2998, 2999, 3000, 3001, 3002, 3003, 3004, 3005, 3005,
                    3005, 3019, 3020, 3022, 3023, 3024, 3025, 3026, 3026, 3027, 3027, 3028, 3028, 3029, 3029, 3030, 3030, 3031,
                    3031, 3032, 3033, 3034, 3035, 3036, 3037, 3037},
            new int[]{3525, 3526, 3527, 3529, 3529, 3534, 3534, 3535, 3536, 3537, 3538, 3539, 3540, 3541, 3542, 3543, 3544,
                    3545, 3545, 3546, 3546, 3545, 3544, 3543, 3543, 3542, 3541, 3540, 3539, 3537, 3536, 3535, 3534, 3533, 3532,
                    3531, 3530, 3529, 3528, 3527, 3526, 3526, 3525},
            43
    );
    private static final Polygon FEROX_ENCLAVE = new Polygon(
            new int[]{3128, 3128, 3123, 3123, 3120, 3120, 3117, 3117, 3119, 3119, 3120, 3120, 3125, 3125, 3127, 3127, 3129,
                    3129, 3137, 3137, 3147, 3147, 3156, 3156, 3161, 3161, 3153, 3153, 3152, 3151, 3150, 3149, 3145, 3145, 3141, 3141},
            new int[]{3609, 3615, 3615, 3620, 3620, 3622, 3622, 3635, 3635, 3640, 3640, 3645, 3645, 3644, 3644, 3645, 3645,
                    3644, 3644, 3646, 3646, 3647, 3647, 3642, 3642, 3626, 3626, 3623, 3623, 3622, 3621, 3620, 3620, 3615, 3615, 3609},
            36
    );
    private static final Cuboid MAIN_WILDERNESS_CUBOID = new Cuboid(2944, 3525, 0, 3391, 4351, 3);
    private static final Cuboid GOD_WARS_WILDERNESS_CUBOID = new Cuboid(3008, 10112, 0, 3071, 10175, 3);
    private static final Cuboid WILDERNESS_UNDERGROUND_CUBOID = new Cuboid(2944, 9920, 0, 3455, 10879, 3);
    private static final Cuboid HUNTERS_END = new Cuboid(1728, 11520, 0, 1791, 11583, 3);
    private static final Cuboid SKELETAL_TOMB = new Cuboid(1856, 11520, 0, 1919, 11583, 3);
    private static final Cuboid WEB_CHASM = new Cuboid(1600, 11520, 0, 1663, 11583, 3);
    private static final Cuboid CALLISTOS_DEN = new Cuboid(3328, 10304, 0, 3391, 10367, 3);
    private static final Cuboid SILK_CHASM = new Cuboid(3392, 10176, 0, 3455, 10239, 3);
    private static final Cuboid VETIONS_REST = new Cuboid(3264, 10176, 0, 3327, 10239, 3);
    private static final Cuboid WILDERNESS_ESCAPE_CAVES = new Cuboid(3328, 10240, 0, 3391, 10303, 3);
    private static final Cuboid WILDERNESS_BH_CRATER = new Cuboid(3328, 3968, 0, 3519, 4159, 3);
    private static final Cuboid WILDERNESS_BH_CRATER_TWO = new Cuboid(3413, 4053, 0, 3434, 4074, 3);

    /**
     * Gets the wilderness level based on a world point
     * Java reimplementation of clientscript 384 [proc,wilderness_level]
     *
     * @param apiPoint the point in the world to get the wilderness level for (Standard API)
     * @return the int representing the wilderness level
     */
    public static int getWildernessLevelFrom(net.runelite.api.coords.WorldPoint apiPoint) {
        // CONVERSION: Convert the Standard API point to Microbot WorldPoint for Cuboid compatibility
        WorldPoint point = new WorldPoint(apiPoint.getX(), apiPoint.getY(), apiPoint.getPlane());

        int regionID = point.getRegionID();
        if (regionID != 12700 && regionID != 12187 && !FEROX_ENCLAVE.contains(point.getX(), point.getY())) {
            if (WILDERNESS_BH_CRATER.contains(point) && !WILDERNESS_BH_CRATER_TWO.contains(point)) {
                return 5;
            } else if (MAIN_WILDERNESS_CUBOID.contains(point)) {
                return NOT_WILDERNESS_BLACK_KNIGHTS.contains(point.getX(), point.getY()) ? 0 : (point.getY() - 3520) / 8 + 1;
            } else if (GOD_WARS_WILDERNESS_CUBOID.contains(point)) {
                return (point.getY() - 9920) / 8 - 1;
            } else if (VETIONS_REST.contains(point)) {
                return 35;
            } else if (SILK_CHASM.contains(point)) {
                return 35;
            } else if (CALLISTOS_DEN.contains(point)) {
                return 40;
            } else if (HUNTERS_END.contains(point)) {
                return 21;
            } else if (SKELETAL_TOMB.contains(point)) {
                return 21;
            } else if (WEB_CHASM.contains(point)) {
                return 29;
            } else if (WILDERNESS_ESCAPE_CAVES.contains(point)) {
                return 33 + (point.getY() % 64 - 6) * 7 / 50;
            } else {
                return WILDERNESS_UNDERGROUND_CUBOID.contains(point) ? (point.getY() - 9920) / 8 + 1 : 0;
            }
        } else {
            return 0;
        }
    }

    /**
     * Checks if the player is in the wilderness
     *
     * @return True if the player is in the wilderness
     */
    public static boolean isInWilderness() {
        return Microbot.getVarbitValue(Varbits.IN_WILDERNESS) == 1;
    }

    /**
     * Determines if another player is attackable based off of wilderness level and combat levels
     *
     * @param rs2Player the player to determine attackability
     * @return returns true if the player is attackable, false otherwise
     */
    public static boolean isAttackable(Rs2PlayerModel rs2Player) {
        int wildernessLevel = 0;

        // FIXED: Using standard API Enum checks instead of Microbot collection wrappers
        if (Microbot.getClient().getWorldType().contains(net.runelite.api.WorldType.DEADMAN)) {
            return true;
        }
        if (Microbot.getClient().getWorldType().contains(net.runelite.api.WorldType.PVP) ||
                Microbot.getClient().getWorldType().contains(net.runelite.api.WorldType.HIGH_RISK)) {
            wildernessLevel += 15;
        }
        if (Microbot.getVarbitValue(Varbits.IN_WILDERNESS) == 1) {
            // This works now because getWildernessLevelFrom accepts the Standard API point
            wildernessLevel += getWildernessLevelFrom(Microbot.getClient().getLocalPlayer().getWorldLocation());
        }
        return wildernessLevel != 0 && Math.abs(Microbot.getClient().getLocalPlayer().getCombatLevel() - rs2Player.getCombatLevel()) <= wildernessLevel;
    }

    // FIXED: Parameter is now explicitly net.runelite.api.Player
    public static boolean isAttackable(net.runelite.api.Player player) {
        return isAttackable(new Rs2PlayerModel(player));
    }

    /**
     * Determines if another player is attackable based off of wilderness level and combat levels
     *
     * @param rs2Player the player to determine attackability
     * @return returns true if the player is attackable, false otherwise
     */
    public static boolean isAttackable(Rs2PlayerModel rs2Player, boolean isDeadManworld, boolean isPvpWorld, int wildernessLevel) {
        return wildernessLevel != 0 && Math.abs(Microbot.getClient().getLocalPlayer().getCombatLevel() - rs2Player.getCombatLevel()) <= wildernessLevel;
    }

    /**
     * Determines if any player is attackable based off of wilderness level and combat levels
     * @return
     */
    public static boolean isAttackable() {
        List<Rs2PlayerModel> players = Rs2Player.getPlayers(player -> true).collect(Collectors.toList());
        int wildernessLevel = 0;

        // FIXED: Using standard API checks
        boolean isDeadManWorld = Microbot.getClient().getWorldType().contains(net.runelite.api.WorldType.DEADMAN);
        boolean isPVPWorld = Microbot.getClient().getWorldType().contains(net.runelite.api.WorldType.PVP);

        if (Microbot.getVarbitValue(Varbits.IN_WILDERNESS) == 1) {
            wildernessLevel += getWildernessLevelFrom(Microbot.getClient().getLocalPlayer().getWorldLocation());
        }
        for (Rs2PlayerModel player: players) {
            if (!isAttackable(player, isDeadManWorld, isPVPWorld, wildernessLevel)) continue;
            System.out.println("Player: " + player.getName() + " with combat " + player.getCombatLevel() + " detected!");
            return true;
        }
        System.out.println("No attackable players detected...");
        return false;
    }

    public static int calculateRisk(net.runelite.api.Client client, ItemManager itemManager) {
        if (client.getItemContainer(InventoryID.EQUIPMENT) == null) {
            return 0;
        }
        if (client.getItemContainer(InventoryID.INVENTORY) == null || client.getItemContainer(InventoryID.INVENTORY).getItems() == null) {
            return 0;
        }

        // FIXED: Explicitly use standard API Item type
        net.runelite.api.Item[] items = ArrayUtils.addAll(Objects.requireNonNull(client.getItemContainer(InventoryID.EQUIPMENT)).getItems(),
                Objects.requireNonNull(client.getItemContainer(InventoryID.INVENTORY)).getItems());

        TreeMap<Integer, net.runelite.api.Item> priceMap = new TreeMap<>(Comparator.comparingInt(Integer::intValue));
        int wealth = 0;

        for (net.runelite.api.Item i : items) {
            int value = (itemManager.getItemPrice(i.getId()) * i.getQuantity());

            // FIXED: Explicitly use standard API ItemComposition
            final net.runelite.api.ItemComposition itemComposition = itemManager.getItemComposition(i.getId());
            if (!itemComposition.isTradeable() && value == 0) {
                value = itemComposition.getPrice() * i.getQuantity();
                priceMap.put(value, i);
            } else {
                value = itemManager.getItemPrice(i.getId()) * i.getQuantity();
                if (i.getId() > 0 && value > 0) {
                    priceMap.put(value, i);
                }
            }
            wealth += value;
        }
        return Integer.parseInt(QuantityFormatter.quantityToRSDecimalStack(priceMap.keySet().stream().mapToInt(Integer::intValue).sum()));
    }
}