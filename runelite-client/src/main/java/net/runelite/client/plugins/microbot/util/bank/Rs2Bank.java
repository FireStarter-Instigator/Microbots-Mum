package net.runelite.client.plugins.microbot.util.bank;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.RuneScapeProfileType;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.shortestpath.ShortestPathPlugin;
import net.runelite.client.plugins.microbot.shortestpath.pathfinder.Pathfinder;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.bank.enums.BankLocation;
import net.runelite.client.plugins.microbot.util.coords.Rs2WorldPoint;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.grandexchange.GrandExchangeSlots;
import net.runelite.client.plugins.microbot.util.grandexchange.GrandExchangeWidget;
import net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.inventory.RunePouchType;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;
import net.runelite.client.plugins.microbot.util.menu.NewMenuEntry;
import net.runelite.client.plugins.microbot.util.misc.Predicates;
import net.runelite.client.plugins.microbot.util.misc.Rs2UiHelper;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.security.Encryption;
import net.runelite.client.plugins.microbot.util.security.Login;
import net.runelite.client.plugins.microbot.util.settings.Rs2Settings;
import net.runelite.client.plugins.microbot.util.tile.Rs2Tile;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import org.apache.commons.lang3.tuple.Pair;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.runelite.api.Varbits.*;
import static net.runelite.api.widgets.ComponentID.BANK_INVENTORY_ITEM_CONTAINER;
import static net.runelite.client.plugins.microbot.Microbot.updateItemContainer;
import static net.runelite.client.plugins.microbot.util.Global.*;
import static net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject.hoverOverObject;
import static net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange.attemptSellWithTimeout;
import static net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange.sellLoot;
import static net.runelite.client.plugins.microbot.util.npc.Rs2Npc.hoverOverActor;

@SuppressWarnings("unused")
@Slf4j
public class Rs2Bank {
    /**
     * 12:12
     */
    public static final int BANK_ITEM_CONTAINER = 786444;
    public static final int BANK_ITEM_WIDTH = 36;
    public static final int BANK_ITEM_HEIGHT = 32;
    public static final int BANK_ITEM_Y_PADDING = 4;
    public static final int BANK_ITEMS_PER_ROW = 8;
    private static final int X_AMOUNT_VARBIT = VarbitID.BANK_REQUESTEDQUANTITY;
    private static final int SELECTED_OPTION_VARBIT = VarbitID.BANK_QUANTITY_TYPE;

    private static final int WITHDRAW_AS_NOTE_VARBIT = 3958;

    // Bank data caching system
    private static final String CONFIG_GROUP = "microbot";
    private static final String BANK_KEY = "bankitems";
    private static final Rs2BankData rs2BankData = new Rs2BankData();
    private static final Gson gson = new Gson();
    private static String rsProfileKey;
    private static RuneScapeProfileType worldType;
    private static boolean loggedInStateKnown = false;
    // Used to synchronize calls
    private static final Object lock = new Object();
    /**
     * Container describes from what interface the action happens
     * eg: withdraw means the contailer will be the bank container
     * eg: deposit means that the container will be the inventory container
     * and so on...
     */
    private static int container = -1;
    // Array to store the counts of items in each tab
    private static final int[] bankTabCounts = new int[9];

    /**
     * Executes menu swapping for a specific rs2Item and entry index.
     *
     * @param identifier The index of the entry to swap.
     * @param rs2Item    The ItemWidget associated with the menu swap.
     */
    public static void invokeMenu(final int identifier, Rs2ItemModel rs2Item) {
        Rectangle itemBoundingBox = null;

        if (container == BANK_INVENTORY_ITEM_CONTAINER) {
            itemBoundingBox = Rs2Inventory.itemBounds(rs2Item);
        }
        if (container == BANK_ITEM_CONTAINER) {
            int itemTab = getItemTabForBankItem(rs2Item.getSlot());
            if (!isTabOpen(itemTab))
                openTab(itemTab);
            scrollBankToSlot(rs2Item.getSlot());
            itemBoundingBox = itemBounds(rs2Item);
        }

        Microbot.doInvoke(new NewMenuEntry(rs2Item.getSlot(), container, MenuAction.CC_OP.getId(), identifier, rs2Item.getId(), rs2Item.getName()), (itemBoundingBox == null) ? new Rectangle(1, 1) : itemBoundingBox);
        // MenuEntryImpl(getOption=Wear, getTarget=<col=ff9040>Amulet of glory(4)</col>, getIdentifier=9, getType=CC_OP_LOW_PRIORITY, getParam0=1, getParam1=983043, getItemId=1712, isForceLeftClick=false, isDeprioritized=false)
        // Rs2Reflection.invokeMenu(rs2Item.slot, container, MenuAction.CC_OP.getId(), identifier, rs2Item.id, "Withdraw-1", rs2Item.name, -1, -1);
    }

    /**
     * Gets the bounding rectangle for the slot of the specified item in the bank container.
     *
     * @param rs2Item The item to get the bounds for.
     *
     * @return The bounding rectangle for the item's slot, or null if the item is not found.
     */
    public static Rectangle itemBounds(Rs2ItemModel rs2Item) {
        Widget itemWidget = getItemWidget(rs2Item.getSlot());

        if (itemWidget == null) return null;

        return itemWidget.getBounds();
    }

    /**
     * Checks whether the bank interface is open.
     *
     * @return {@code true} if the bank interface is open, {@code false} otherwise.
     */
    public static boolean isOpen() {
        if (!handleBankPin()) return false;
        return Rs2Widget.isWidgetVisible(12, 1);
    }

    public static List<Rs2ItemModel> bankItems() {
        return rs2BankData.getBankItems();
    }

    /**
     * Closes the bank interface if it is open.
     *
     * @return true if the bank interface was open and successfully closed, true if already closed.
     */
    public static boolean closeBank() {
        if (!isOpen()) return true;
        if (Rs2Settings.isEscCloseInterfaceSettingEnabled()) {
            Rs2Keyboard.keyPress(KeyEvent.VK_ESCAPE);
        } else {
            Rs2Widget.clickChildWidget(786434, 11);
        }

        return sleepUntil(() -> !isOpen(), 5000);
    }

    /**
     * Finds a bank item widget in the bank interface by its partial name match.
     *
     * @param name The name of the item to find.
     *
     * @return The bank item widget if found, or null if not found.
     */
    public static Rs2ItemModel findBankItem(String name) {
        return findBankItem(name, false);
    }

    /**
     * check if the player has a bank item identified by id
     *
     * @param id the item id
     *
     * @return boolean
     */
    public static boolean hasItem(int id) {
        return findBankItem(id) != null;
    }

    /**
     * check if the player has a bank item identified by contains name
     *
     * @param name the item name
     *
     * @return boolean
     */
    public static boolean hasItem(String name) {
        return hasItem(name, false);
    }

    /**
     * @param name
     * @param exact
     *
     * @return
     */
    public static boolean hasItem(String name, boolean exact) {
        return findBankItem(name, exact) != null;
    }

    /**
     * Checks if the bank contains any of the specified item names.
     *
     * @param names A list of item names to check for.
     * @return True if any of the items are found, false otherwise.
     */
    public static boolean hasItem(List<String> names) {
        return hasItem(names, false, 1);
    }

    /**
     * Checks if the bank contains any of the specified item names.
     *
     * @param names A list of item names to check for.
     * @param exact If true, requires an exact name match.
     * @return True if any of the items are found, false otherwise.
     */
    public static boolean hasItem(List<String> names, boolean exact) {
        return hasItem(names, exact, 1);
    }

    /**
     * Checks if the bank contains any of the specified item names.
     *
     * @param names A list of item names to check for.
     * @param amount The minimum quantity required for each item.
     * @return True if any of the items are found, false otherwise.
     */
    public static boolean hasItem(List<String> names, int amount) {
        return hasItem(names, false, amount);
    }

    /**
     * Checks if the bank contains all items from a list of names with a minimum quantity.
     *
     * @param names  A list of item names to check for.
     * @param exact  If true, requires an exact name match.
     * @param amount The minimum quantity required for each item.
     * @return True if all items from the list exist in the bank with the required quantity, false otherwise.
     */
    public static boolean hasAllItems(List<String> names, boolean exact, int amount) {
        return names.stream().allMatch(name -> {
            Rs2ItemModel item = findBankItem(name, exact, amount);
            return item != null;
        });
    }

    /**
     * Checks if the bank contains any items from a list of names with a minimum quantity.
     *
     * @param names  A list of item names to check for.
     * @param exact  If true, requires an exact name match.
     * @param amount The minimum quantity required for the items.
     * @return True if the bank contains at least one of the items with the specified quantity, false otherwise.
     */
    public static boolean hasItem(List<String> names, boolean exact, int amount) {
        return findBankItem(names, exact, amount) != null;
    }

    /**
     * Checks if the bank contains any item from the given array of IDs.
     *
     * @param ids The array of item IDs to check.
     * @return True if the bank contains at least one of the specified items, false otherwise.
     */
    public static boolean hasItem(int[] ids) {
        return Arrays.stream(ids)
                .anyMatch(id -> findBankItem(id) != null);
    }

    /**
     * Checks if the bank contains all items from the given array of IDs.
     *
     * @param ids The array of item IDs to check.
     * @return True if the bank contains all the specified items, false otherwise.
     */
    public static boolean hasAllItems(int[] ids) {
        return Arrays.stream(ids)
                .allMatch(id -> findBankItem(id) != null);
    }

    /**
     * Checks if the bank contains any item from the given array of IDs with the specified quantity.
     *
     * @param ids The array of item IDs to check.
     * @param amount The minimum quantity required for each item.
     * @return True if the bank contains at least one of the specified items with the required quantity, false otherwise.
     */
    public static boolean hasItem(int[] ids, int amount) {
        return Arrays.stream(ids)
                .anyMatch(id -> {
                    Rs2ItemModel item = findBankItem(id);
                    return item != null && item.getQuantity() >= amount;
                });
    }

    /**
     * Checks if the bank contains all items from the given array of IDs with the specified quantity.
     *
     * @param ids The array of item IDs to check.
     * @param amount The minimum quantity required for each item.
     * @return True if the bank contains all the specified items with the required quantity, false otherwise.
     */
    public static boolean hasAllItems(int[] ids, int amount) {
        return Arrays.stream(ids)
                .allMatch(id -> {
                    Rs2ItemModel item = findBankItem(id);
                    return item != null && item.getQuantity() >= amount;
                });
    }

    /**
     * check if the player has a bank item identified by exact name.
     *
     * @param name the item name
     *
     * @return boolean
     */
    public static boolean hasBankItem(String name) {
        return findBankItem(name, false, 1) != null;
    }

    /**
     * check if the player has a bank item identified by exact name.
     *
     * @param name the item name
     *
     * @return boolean
     */
    public static boolean hasBankItem(String name, int amount) {
        return hasBankItem(name, amount, false);
    }

    /**
     * check if the player has a bank item identified by exact name.
     *
     * @param name the item name
     *
     * @return boolean
     */
    public static boolean hasBankItem(String name, int amount, boolean exact) {
        return findBankItem(name, exact, amount) != null;
    }

    /**
     * check if the player has a bank item identified by exact name.
     *
     * @param name  the item name
     * @param exact exact search based on equalsIgnoreCase
     *
     * @return boolean
     */
    public static boolean hasBankItem(String name, boolean exact) {
        return findBankItem(name, exact) != null;
    }

    //hasBankItem overload to check with id and amount
    public static boolean hasBankItem(int id, int amount) {
        return count(id) >= amount;
    }

    /**
     * Query count of item inside of bank
     */
    public static int count(int id) {
        Rs2ItemModel bankItem = findBankItem(id);
        if (bankItem == null) return 0;
        return bankItem.getQuantity();
    }

    /**
     * Query count of item inside of bank
     */
    public static int count(String name, boolean exact) {
        Rs2ItemModel bankItem = findBankItem(name, exact);
        if (bankItem == null) return 0;
        return bankItem.getQuantity();
    }

    /**
     * Query count of item inside of bank
     */
    public static int count(String name) {
        return count(name, false);
    }

    /**
     * Deposits all equipped items into the bank.
     * This method finds and clicks the "Deposit Equipment" button in the bank interface.
     */
    public static boolean depositEquipment() {
        Widget widget = Rs2Widget.findWidget(SpriteID.BANK_DEPOSIT_EQUIPMENT, null);
        if (widget == null) {Rs2Widget.clickWidget(786478); return false;}

        Microbot.getMouse().click(widget.getBounds());
        return true;
    }

    /**
     * Deposits one item quickly into the bank by its ItemWidget.
     *
     * @param rs2Item The ItemWidget representing the item to deposit.
     */
    private static boolean depositOne(Rs2ItemModel rs2Item) {
        if (rs2Item == null) return false;
        if (!isOpen()) return false;
        if (!Rs2Inventory.hasItem(rs2Item.getId())) return false;
        container = BANK_INVENTORY_ITEM_CONTAINER;

        if (Microbot.getVarbitValue(SELECTED_OPTION_VARBIT) == 0) {
            invokeMenu(2, rs2Item);
        } else {
            invokeMenu(3, rs2Item);
        }
        return true;
    }

    /**
     * Deposits one item quickly by its ID.
     *
     * @param id The ID of the item to deposit.
     */
    public static boolean depositOne(int id) {
        return depositOne(Rs2Inventory.get(id));
    }

    /**
     * Deposits one item quickly by its name with a partial name match.
     *
     * @param name The name of the item to deposit.
     */
    public static boolean depositOne(String name, boolean exact) {
        return depositOne(Rs2Inventory.get(name, exact));
    }

    /**
     * Deposits one item quickly by its name with a partial name match.
     *
     * @param name The name of the item to deposit.
     */
    public static boolean depositOne(String name) {
        return depositOne(name, false);
    }

    /**
     * Deposits a specified amount of an item into the inventory.
     * This method checks if the bank window is open, if the provided ItemWidget is valid and
     * if the player has the item in their inventory. If all conditions are met, it calls the
     * 'handleAmount' method to deposit the specified amount of the item into the inventory.
     *
     * @param rs2Item item to handle
     * @param amount  amount to deposit
     */
    private static boolean depositX(Rs2ItemModel rs2Item, int amount) {
        if (rs2Item == null) return false;
        if (!isOpen()) return false;
        if (!Rs2Inventory.hasItem(rs2Item.getId())) return false;
        container = BANK_INVENTORY_ITEM_CONTAINER;

        return handleAmount(rs2Item, amount);
    }

    /**
     * Handles the amount for an item widget.
     * <p>
     * This method checks if the current varbit value matches the specified amount.
     * If it does, it executes the menu swapper with the HANDLE_X_SET option.
     * If it doesn't match, it executes the menu swapper with the HANDLE_X_UNSET option,
     * enters the specified amount using the VirtualKeyboard, and presses Enter.
     *
     * @param rs2Item The item to handle.
     * @param amount  The desired amount to set.
     */
    private static boolean handleAmount(Rs2ItemModel rs2Item, int amount) {
        return handleAmount(rs2Item, amount, false);
    }

    /**
     * Handles the amount for an item widget.
     * <p>
     * This method checks if the current varbit value matches the specified amount.
     * If it does, it executes the menu swapper with the HANDLE_X_SET option.
     * If it doesn't match, it executes the menu swapper with the HANDLE_X_UNSET option,
     * enters the specified amount using the VirtualKeyboard, and presses Enter.
     *
     * @param rs2Item The item to handle.
     * @param amount  The desired amount to set.
     * @param safe    will wait for item to appear in inventory before continuing if set to true
     */
    private static boolean handleAmount(Rs2ItemModel rs2Item, int amount, boolean safe) {

        if (amount <= 0) return true;
        int selected = Microbot.getVarbitValue(VarbitID.BANK_QUANTITY_TYPE);
        int configuredX = Microbot.getVarbitValue(VarbitID.BANK_REQUESTEDQUANTITY);
        boolean hasX = configuredX > 0;
        boolean isInventory = (container == BANK_INVENTORY_ITEM_CONTAINER);
        int xSetOffset = -1;
        int xPromptOffset = isInventory ? 7 : 6;

        if (hasX) {
            switch (selected) {
                case 0:
                case 1:
                case 2:
                case 4:
                    xSetOffset = isInventory ? 6 : 5;
                    break;
                case 3:
                    xSetOffset = isInventory ? 2 : 1;
                    break;
                default:
                    throw new IllegalStateException("Unknown BANK_QUANTITY_TYPE: " + selected);
            }
        }

        if (hasX && configuredX == amount) {
            final int before = Rs2Inventory.count();
            invokeMenu(xSetOffset, rs2Item);
            if (safe) return sleepUntilTrue(() -> Rs2Inventory.count() != before, 100, 2500);
            return true;
        }

        invokeMenu(xPromptOffset, rs2Item);
        boolean foundEnterAmount = sleepUntil(() -> {
            Widget widget = Rs2Widget.getWidget(162, 42);
            return widget != null && widget.getText().equalsIgnoreCase("Enter amount:");
        }, 5000);
        if (!foundEnterAmount) return false;
        Rs2Random.waitEx(1200, 100);
        Rs2Keyboard.typeString(String.valueOf(amount));
        Rs2Keyboard.enter();

        if (safe) return sleepUntilTrue(() -> isInventory != Rs2Inventory.hasItem(rs2Item.getId()), 100, 2500);

        return true;
    }

    /**
     * deposit x amount of items identified by its name
     * set exact to true if you want to identify by its exact name
     *
     * @param id param amount
     */
    public static boolean depositX(int id, int amount) {
        return depositX(Rs2Inventory.get(id), amount);
    }

    /**
     * deposit x amount of items identified by its name
     * set exact to true if you want to identify by its exact name
     *
     * @param name param amount
     *             param exact
     */
    private static boolean depositX(String name, int amount, boolean exact) {
        return depositX(Rs2Inventory.get(name, exact), amount);
    }

    /**
     * deposit x amount of items identified by its name
     *
     * @param name param amount
     */
    public static boolean depositX(String name, int amount) {
        return depositX(Rs2Inventory.get(name), amount);
    }

    /**
     * deposit all items identified by its ItemWidget
     *
     * @param rs2Item item to deposit
     *
     * @returns did deposit anything
     */
    private static boolean depositAll(Rs2ItemModel rs2Item) {
        if (rs2Item == null) return false;
        if (!isOpen()) return false;
        if (!Rs2Inventory.hasItem(rs2Item.getId())) return false;
        container = BANK_INVENTORY_ITEM_CONTAINER;

        if (Microbot.getVarbitValue(SELECTED_OPTION_VARBIT) == 4) {
            invokeMenu(2, rs2Item);
        } else {
            invokeMenu(8, rs2Item);
        }
        return true;
    }

    /**
     * deposit all items identified by its id
     *
     * @param id searches based on the id
     *
     * @return true if anything deposited
     */
    public static boolean depositAll(int id) {
        Rs2ItemModel rs2Item = Rs2Inventory.get(id);
        if (rs2Item == null) return false;
        return depositAll(rs2Item);
    }

    public static boolean depositAll(Predicate<Rs2ItemModel> predicate) {
        boolean result = false;
        List<Rs2ItemModel> items = Rs2Inventory.items(predicate).distinct().collect(Collectors.toList());
        for (Rs2ItemModel item : items) {
            if (item == null) continue;
            depositAll(item);
            sleep(Rs2Random.randomGaussian(400,200));
            result = true;
        }
        return result;
    }

    // boolean to determine if we still have items to deposit
    private static boolean isDepositing(Predicate<Rs2ItemModel> filter) {
        List<Rs2ItemModel> itemsToDeposit = Rs2Inventory.all(filter)
                .stream()
                .filter(Objects::nonNull)
                .filter(Predicates.distinctByProperty(Rs2ItemModel::getName))
                .collect(Collectors.toList());

        return !itemsToDeposit.isEmpty();
    }

    /**
     * deposit all items identified by its name
     * set exact to true if you want to be identified by its exact name
     *
     * @param name  name to search
     * @param exact does an exact search equalsIgnoreCase
     */
    public static boolean depositAll(String name, boolean exact) {
        return depositAll(Rs2Inventory.get(name, exact));
    }

    /**
     * deposit all items identified by its name
     *
     * @param name item name to search
     */
    public static boolean depositAll(String name) {
        return depositAll(name, false);
    }

    /**
     * deposit all items
     */
    public static boolean depositAll() {
        Microbot.status = "Deposit all";
        if (Rs2Inventory.isEmpty()) return true;
        if (!Rs2Bank.isOpen()) return false;

        Widget widget = Rs2Widget.findWidget(SpriteID.BANK_DEPOSIT_INVENTORY, null);
        if (widget == null) return false;

        Rs2Widget.clickWidget(widget);
        return Rs2Inventory.waitForInventoryChanges(10_000);
    }

    /**
     * Deposits all items in the player's inventory into the bank, except for the items with the specified IDs.
     * This method uses a lambda function to filter out the items with the specified IDs from the deposit operation.
     *
     * @param ids The IDs of the items to be excluded from the deposit.
     *
     * @return true if any items were deposited, false otherwise.
     */
    public static boolean depositAllExcept(Integer... ids) {
        return depositAll(x -> Arrays.stream(ids).noneMatch(id -> id == x.getId()));
    }

    /**
     * Deposits all items in the player's inventory into the bank, except for the items with the specified names.
     * This method uses a lambda function to filter out the items with the specified names from the deposit operation.
     *
     * @param names The names of the items to be excluded from the deposit.
     *
     * @return true if any items were deposited, false otherwise.
     */
    public static boolean depositAllExcept(String... names) {
        return depositAll(x -> Arrays.stream(names).noneMatch(name -> name.equalsIgnoreCase(x.getName())));
    }

    /**
     * Deposits all items in the player's inventory into the bank, except for the items with the specified names.
     * This method uses a lambda function to filter out the items with the specified names from the deposit operation.
     *
     * @param names The names of the items to be excluded from the deposit.
     *
     * @return true if any items were deposited, false otherwise.
     */
    public static boolean depositAllExcept(List<String> names) {
        return depositAll(x -> names.stream().noneMatch(name -> name.equalsIgnoreCase(x.getName())));
    }

    /**
     * Deposits all items in the player's inventory into the bank,
     * except for the items in the given map.
     * Each key is the item name, and the value indicates whether to fuzzy match it.
     *
     * @param itemsToExclude A map of item names to a boolean indicating fuzzy match.
     * @return true if any items were deposited, false otherwise.
     */
    public static boolean depositAllExcept(Map<String, Boolean> itemsToExclude) {
        return depositAll(item -> itemsToExclude.entrySet().stream().noneMatch(entry -> {
            String excludedItemName = entry.getKey();
            boolean isFuzzy = entry.getValue();
            return isFuzzy
                    ? item.getName().toLowerCase().contains(excludedItemName.toLowerCase())
                    : item.getName().equalsIgnoreCase(excludedItemName);
        }));
    }

    /**
     * Deposits all items in the player's inventory into the bank, except for the items with the specified names.
     * This method uses a lambda function to filter out the items with the specified names from the deposit operation.
     * It also allows for a delay between deposit operations.
     *
     * @param names The names of the items to be excluded from the deposit.
     *
     * @return true if any items were deposited, false otherwise.
     */
    public static boolean depositAllExcept(boolean exact, String... names) {
        if (!exact)
            return depositAll(x -> Arrays.stream(names).noneMatch(name -> x.getName().toLowerCase().contains(name.toLowerCase())));
        else
            return depositAll(x -> Arrays.stream(names).noneMatch(name -> name.equalsIgnoreCase(x.getName())));
    }

    /**
     * withdraw one item identified by its ItemWidget.
     *
     * @param rs2Item item to withdraw
     */
    private static boolean withdrawOne(Rs2ItemModel rs2Item) {
        if (rs2Item == null) {Microbot.log("bank item is null and so we can't withdraw it"); return false;}
        if (!isOpen()) return false;
        if (Rs2Inventory.isFull()) return false;
        container = BANK_ITEM_CONTAINER;

        final int entryIndex = Microbot.getVarbitValue(SELECTED_OPTION_VARBIT) == 0 ? 1 : 2;
        invokeMenu(entryIndex, rs2Item);
        return true;
    }

    /**
     * withdraw one item identified by its id.
     *
     * @param id the item id
     */
    public static boolean withdrawOne(int id) {
        return withdrawOne(findBankItem(id));
    }

    public static boolean withdrawItem(String name) {
        return withdrawOne(name);
    }

    public static boolean withdrawItem(int id) {
        return withdrawOne(id);
    }

    public static boolean withdrawItem(boolean checkInv, int id) {
        if (checkInv && Rs2Inventory.hasItem(id)) return true;
        return withdrawOne(id);
    }

    public static boolean withdrawItem(boolean checkInv, String name) {
        if (checkInv && Rs2Inventory.hasItem(name)) return true;
        return withdrawOne(name);
    }

    /**
     * withdraw one item identified by its name.
     * set exact to true if you want to identify by the exact name.
     *
     * @param name  the item name
     * @param exact boolean
     */
    public static boolean withdrawOne(String name, boolean exact) {
        return withdrawOne(findBankItem(name, exact));
    }

    /**
     * withdraw one item identified by its name
     *
     * @param name the item name
     */
    public static boolean withdrawOne(String name) {
        return withdrawOne(name, false);
    }

    @Deprecated(since="Use withdrawOne & sleep")
    public static boolean withdrawOne(String name, int sleepTime) {
        final boolean success = withdrawOne(name, false);
        sleep(sleepTime);
        return success;
    }

    /**
     * withdraw one item identified by its id.
     *
     * @param id the item id
     */
    public static boolean withdrawAllButOne(int id) {
        return withdrawAllButOne(findBankItem(id));
    }

    /**
     * withdraw one item identified by its name
     *
     * @param name the item name
     */
    public static boolean withdrawAllButOne(String name) {
        return withdrawAllButOne(name, false);
    }


    /**
     * withdraw one item identified by its name.
     * set exact to true if you want to identify by the exact name.
     *
     * @param name  the item name
     * @param exact boolean
     */
    public static boolean withdrawAllButOne(String name, boolean exact) {
        return withdrawAllButOne(findBankItem(name, exact));
    }

    /**
     * withdraw all but one of an item identified by its ItemWidget.
     *
     * @param rs2Item item to withdraw
     */
    private static boolean withdrawAllButOne(Rs2ItemModel rs2Item) {
        if (rs2Item == null) return false;
        if (!isOpen()) return false;
        if (Rs2Inventory.isFull()) return true;
        container = BANK_ITEM_CONTAINER;

        invokeMenu(7, rs2Item);
        return true;
    }

    /**
     * withdraw x amount of items identified by its ItemWidget.
     *
     * @param rs2Item Item to handle
     * @param amount  int
     */
    private static boolean withdrawXItem(Rs2ItemModel rs2Item, int amount) {
        if (!isOpen()) return false;
        if (rs2Item == null) return false;
        if (Rs2Inventory.isFull() && !Rs2Inventory.hasItem(rs2Item.getId()) && !rs2Item.isStackable()) return false;
        container = BANK_ITEM_CONTAINER;

        return handleAmount(rs2Item, amount);
    }

    /**
     * Withdraws the deficit of an item from the bank to meet the required amount.
     *
     * @param id             The ID of the item to withdraw.
     * @param requiredAmount The required total amount of the item.
     * @return True if any items were withdrawn, false otherwise.
     */
    public static boolean withdrawDeficit(int id, int requiredAmount) {
        int currentAmount = Rs2Inventory.itemQuantity(id);
        int deficit = requiredAmount - currentAmount;

        if (deficit <= 0) return true;
        if (!hasBankItem(id, deficit)) return false;

        return withdrawX(id, deficit);
    }

    public static boolean withdrawDeficit(String name, int requiredAmount, boolean exact) {
        final int deficit = requiredAmount - Rs2Inventory.itemQuantity(name);
        if (deficit <= 0) return true;
        return hasBankItem(name, deficit, exact) && withdrawX(name, deficit, exact);
    }

    /**
     * Withdraws the deficit of an item from the bank to meet the required amount.
     *
     * @param name           The name of the item to withdraw.
     * @param requiredAmount The required total amount of the item.
     * @return True if any items were withdrawn, false otherwise.
     */
    public static boolean withdrawDeficit(String name, int requiredAmount) {
        return withdrawDeficit(name, requiredAmount, false);
    }

    /**
     * Checks inventory before withdrawing item
     *
     * @param checkInv check inventory before withdrawing item
     * @param id       item id
     * @param amount   amount to withdraw
     */
    public static boolean withdrawX(boolean checkInv, int id, int amount) {
        if (checkInv && Rs2Inventory.hasItemAmount(id, amount)) return true;
        return withdrawX(id, amount);
    }

    /**
     * Checks inventory before withdrawing item
     *
     * @param checkInv check inventory before withdrawing item
     * @param name     item name
     * @param amount   amount to withdraw
     */
    public static boolean withdrawX(boolean checkInv, String name, int amount) {
        return withdrawX(checkInv, name, amount, false);
    }

    /**
     * Checks inventory before withdrawing item
     *
     * @param checkInv check inventory before withdrawing item
     * @param name     item name
     * @param amount   amount to withdraw
     * @param exact    exact search based on equalsIgnoreCase
     */
    public static boolean withdrawX(boolean checkInv, String name, int amount, boolean exact) {
        if (checkInv && Rs2Inventory.hasItemAmount(name, amount)) return true;
        return withdrawX(name, amount, exact);
    }

    /**
     * withdraw x amount of items identified by its id.
     *
     * @param id     item id to search
     * @param amount amount to withdraw
     */
    public static boolean withdrawX(int id, int amount) {
        return withdrawXItem(findBankItem(id), amount);
    }

    /**
     * withdraw x amount of items identified by its name.
     * set exact to true if you want to identify an item by its exact name.
     *
     * @param name   item name to search
     * @param amount amount to withdraw
     * @param exact  exact search based on equalsIgnoreCase
     */
    public static boolean withdrawX(String name, int amount, boolean exact) {
        return withdrawXItem(findBankItem(name, exact,amount), amount);
    }

    /**
     * withdraw x amount of items identified by its name
     *
     * @param name   item name to search
     * @param amount amount to withdraw
     */
    public static boolean withdrawX(String name, int amount) {
        return withdrawXItem(findBankItem(name, false,amount), amount);
    }

    /**
     * withdraw all items identified by its ItemWidget.
     *
     * @param rs2Item Item to withdraw
     *
     * @return
     */
    private static boolean withdrawAll(Rs2ItemModel rs2Item) {
        if (!isOpen()) return false;
        if (rs2Item == null) return false;
        if (Rs2Inventory.isFull()) return false;
        container = BANK_ITEM_CONTAINER;

        if (Microbot.getVarbitValue(SELECTED_OPTION_VARBIT) == 4) {
            invokeMenu(1, rs2Item);
        } else {
            invokeMenu(7, rs2Item);
        }
        return true;
    }

    /**
     * withdraw all items identified by its ItemWidget.
     *
     * @param rs2Item Item to withdraw
     *
     * @return
     */
    public static boolean withdrawXHard(Rs2ItemModel rs2Item, int amount) {
        if (!isOpen()) return false;
        if (rs2Item == null) return false;
        if (Rs2Inventory.isFull()) return false;
        container = BANK_ITEM_CONTAINER;

            invokeMenu(5, rs2Item);

        boolean foundEnterAmount = sleepUntil(() -> {
            Widget widget = Rs2Widget.getWidget(162, 42);
            return widget != null && widget.getText().equalsIgnoreCase("Enter amount:");
        }, 5000);
        if (!foundEnterAmount) return false;

        Rs2Random.waitEx(1200, 100);
        Rs2Keyboard.typeString(String.valueOf(amount));
        Rs2Keyboard.enter();
        Rs2Inventory.waitForInventoryChanges(2000);
        return true;
    }

    public static boolean withdrawAll(boolean checkInv, String name) {
        return withdrawAll(checkInv, name, false);
    }

    /**
     * withdraw all items identified by its name.
     *
     * @param checkInv check if item is already in inventory
     * @param name     item name to search
     * @param exact    name
     */
    public static boolean withdrawAll(boolean checkInv, String name, boolean exact) {
        // We don't want an amount check for all bc it is not always 28
        if (checkInv && Rs2Inventory.isFull()) return true;
        return withdrawAll(findBankItem(name, exact));
    }

    /**
     * @param name
     */
    public static boolean withdrawAll(String name) {
        return withdrawAll(false, name, false);
    }

    /**
     * withdraw all items identified by its id.
     *
     * @param id item id to search
     *
     * @return
     */
    public static boolean withdrawAll(int id) {
        return withdrawAll(findBankItem(id));
    }

    /**
     * withdraw all items identified by its name
     * set the boolean exact to true if you want to identify the item by the exact name
     *
     * @param name  item name to search
     * @param exact exact search based on equalsIgnoreCase
     */
    public static boolean withdrawAll(String name, boolean exact) {
        return withdrawAll(findBankItem(name, exact));
    }

    /**
     * wear an item identified by its ItemWidget.
     *
     * @param rs2Item item to wear
     */
    private static boolean wearItem(Rs2ItemModel rs2Item) {
        if (rs2Item == null) return false;
        if (!isOpen()) return false;
        container = BANK_INVENTORY_ITEM_CONTAINER;

        invokeMenu(9, rs2Item);
        return true;
    }

    /**
     * wear an item identified by the name contains
     *
     * @param name item name to search based on contains(string)
     */
    public static boolean wearItem(String name) {
        return wearItem(Rs2Inventory.get(name, false));
    }

    /**
     * wear an item identified by its exact name.
     *
     * @param name  item name to search
     * @param exact exact search based on equalsIgnoreCase
     */
    public static boolean wearItem(String name, boolean exact) {
        return wearItem(Rs2Inventory.get(name, exact));
    }

    /**
     * withdraw all and equip item identified by its id.
     *
     * @param id item id
     */
    public static boolean withdrawXAndEquip(int id, int amount) {
        if (Rs2Equipment.isWearing(id)) return true;
        withdrawX(id, amount);
        if (!sleepUntil(() -> Rs2Inventory.hasItem(id))) return false;
        return wearItem(id);
    }

    /**
     * withdraw all and equip item identified by its id.
     *
     * @param name item name
     */
    public static boolean withdrawAllAndEquip(String name) {
        if (Rs2Equipment.isWearing(name)) return true;
        if (!withdrawAll(name)) return false;
        if (!sleepUntil(() -> Rs2Inventory.hasItem(name))) return false;
        return wearItem(name);
    }

    /**
     * withdraw all and equip item identified by its id.
     *
     * @param id item id
     */
    public static boolean withdrawAllAndEquip(int id) {
        if (Rs2Equipment.isWearing(id)) return true;
        if (!withdrawAll(id)) return false;
        if (!sleepUntil(() -> Rs2Inventory.hasItem(id))) return false;
        return wearItem(id);
    }

    /**
     * withdraw and equip item identified by its id.
     *
     * @param name item name
     */
    public static boolean withdrawAndEquip(String name) {
        if (Rs2Equipment.isWearing(name)) return true;
        if (!withdrawOne(name)) return false;
        if (!sleepUntil(() -> Rs2Inventory.hasItem(name), 1800)) return false;
        return wearItem(name);
    }


    /**
     * withdraw and equip item identified by its id.
     *
     * @param id item id
     */
    public static boolean withdrawAndEquip(int id) {
        if (Rs2Equipment.isWearing(id)) return true;
        if (!withdrawOne(id)) return false;
        if (!sleepUntil(() -> Rs2Inventory.hasItem(id))) return false;
        return wearItem(id);
    }

    /**
     * withdraw and equip item identified by its id.
     *
     * @param id item id
     */
    public static boolean withdrawAndEquip1(int id) {
        //if (Rs2Equipment.isWearing(id)) return true;
        if (!withdrawOne(id)) return false;
        if (!sleepUntil(() -> Rs2Inventory.hasItem(id))) return false;
        return wearItem(id);
    }

    /**
     * withdraw items identified by one or more ids
     *
     * @param ids item ids
     */
    public static boolean withdrawItems(int... ids) {
        boolean success = true;
        for (int id : ids) {
            if (!withdrawOne(id)) success = false;
        }
        return success;
    }

    /**
     * withdraw items identified by one or more ids
     *
     * @param ids item ids
     */
    public static void withdrawItemsAll(int... ids) {
        for (int id : ids) {
            withdrawAll(id);
        }
    }

    /**
     * Deposit items identified by one or more ids
     *
     * @param ids item ids
     */
    public static boolean depositItems(int... ids) {
        boolean success = true;
        for (int id : ids) {
            if (!depositOne(id)) success = false;
        }
        return success;
    }

    /**
     * Find the closest available bank
     * finds closest npc then bank booth then chest
     * @return True if bank was successfully opened, otherwise false.
     */
    public static boolean openBank() {
        Microbot.status = "Opening bank";

        try {
            if (Microbot.getClient().isWidgetSelected()) {
                Microbot.getMouse().click();
            }

            if (isOpen()) return true;

            final Player player = Microbot.getClient().getLocalPlayer();
            if (player == null) return false;
            WorldPoint anchor = player.getWorldLocation();

            List<TileObject> candidates = Stream.of(
                            Rs2GameObject.findBank(),
                            Rs2GameObject.findGrandExchangeBooth()
                    )
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            Optional<TileObject> nearestObj = Rs2GameObject.pickClosest(
                    candidates,
                    TileObject::getWorldLocation,
                    anchor
            );

            if (nearestObj.isPresent()) {
                if (!Rs2GameObject.interact(nearestObj.get(), "Bank")) return false;
            } else {
                final Rs2NpcModel banker = Rs2Npc.getBankerNPC();
                if (!Rs2Npc.interact(banker, "Bank")) return false;
            }

            return sleepUntil(Rs2Bank::isOpen, 5_000);
        } catch (Exception ex) {
            Microbot.logStackTrace("Rs2Bank", ex);
            return false;
        }
    }

    /**
     * Find closest available bank
     * finds closest npc then bank booth then chest
     * @return True if bank was successfully opened, otherwise false.
     */
    public static boolean openBank1() {
        Microbot.status = "Opening bank";

        try {
            if (Microbot.getClient().isWidgetSelected()) {
                Microbot.getMouse().click();
            }

            Player player = Microbot.getClient().getLocalPlayer();
            if (player == null) return false;
            WorldPoint anchor = player.getWorldLocation();

            List<TileObject> candidates = Stream.of(
                            Rs2GameObject.findBank(),
                            Rs2GameObject.findGrandExchangeBooth()
                    )
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            Optional<TileObject> nearestObj = Rs2GameObject.pickClosest(
                    candidates,
                    TileObject::getWorldLocation,
                    anchor
            );

            boolean action = false;
            if (nearestObj.isPresent()) {
                action = Rs2GameObject.interact(nearestObj.get(), "Bank");
            } else {
                Rs2NpcModel banker = Rs2Npc.getBankerNPC();
                if (banker != null) {
                    action = Rs2Npc.interact(banker, "Bank");
                }
            }

            if (action) {
                sleepUntil(Rs2Bank::isOpen, 5000);
            }
            return action;
        } catch (Exception ex) {
            Microbot.logStackTrace("Rs2Bank", ex);
            return false;
        }
    }

    /**
     * Opens the Bank Collection Box in the game if it is not already open.
     * The method determines the closest and most appropriate object or NPC to interact with
     * in order to access the Bank Collection Box. It handles various scenarios such as
     * interacting with a bank, chest, Grand Exchange booth, or NPC banker.
     */
    public static boolean openCollectionBox() {
        Microbot.status = "Opening collection box";

        try {
            if (Microbot.getClient().isWidgetSelected()) {
                Microbot.getMouse().click();
            }

            if (collectionBoxIsOpen()) return true;

            Player player = Microbot.getClient().getLocalPlayer();
            if (player == null) return false;
            WorldPoint anchor = player.getWorldLocation();

            List<TileObject> candidates = Stream.of(
                            Rs2GameObject.findBank(),
                            Rs2GameObject.findGrandExchangeBooth()
                    )
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            Optional<TileObject> nearestObj = Rs2GameObject.pickClosest(
                    candidates,
                    TileObject::getWorldLocation,
                    anchor
            );

            if (nearestObj.isPresent()) {
                if (!Rs2GameObject.interact(nearestObj.get(), "Collect")) return false;
            } else {
                Rs2NpcModel banker = Rs2Npc.getBankerNPC();
                if (banker == null) return false;
                if (!Rs2Npc.interact(banker, "Collect")) return false;
            }

            return sleepUntil(Rs2Bank::collectionBoxIsOpen, 5000);
        } catch (Exception ex) {
            Microbot.logStackTrace("Rs2Bank", ex);
            return false;
        }
    }

    // Add this as a class-level field to persist across method calls
    private static final Set<Integer> untradeableBlacklist = new HashSet<>();
    private static final Set<Integer> unsellableBlacklist = new HashSet<>(); // Items that won't sell on GE
    private static final Map<Integer, Long> offerStartTimes = new HashMap<>(); // Track when offers were placed
    private static final long OFFER_TIMEOUT_MS = 60000; // 60 seconds timeout for stuck offers

    public static boolean sellAllExceptKeepList(List<Integer> keepList, int percent) {
        Microbot.log("=== SELL ALL EXCEPT KEEP LIST CYCLING START ===");
        Microbot.log("Keep list size: " + keepList.size());
        Microbot.log("Percent: " + percent);
        Microbot.log("Untradeable blacklist size: " + untradeableBlacklist.size());
        Microbot.log("Unsellable blacklist size: " + unsellableBlacklist.size());

        // Log the keep list contents
        Microbot.log("=== KEEP LIST CONTENTS ===");
        if (keepList == null) {
            Microbot.log("ERROR: keepList is NULL!");
            return false;
        }

        keepList.forEach(id -> Microbot.log("Keep list contains ID: " + id));

        // Log blacklisted items
        if (!untradeableBlacklist.isEmpty()) {
            Microbot.log("=== BLACKLISTED UNTRADEABLE ITEMS ===");
            untradeableBlacklist.forEach(id -> Microbot.log("Blacklisted ID: " + id));
        }

        if (!unsellableBlacklist.isEmpty()) {
            Microbot.log("=== BLACKLISTED UNSELLABLE ITEMS ===");
            unsellableBlacklist.forEach(id -> Microbot.log("Unsellable ID: " + id));
        }

        // Distance check
        if (Rs2Walker.getDistanceBetween(Rs2Player.getWorldLocation(), new WorldPoint(3164, 3489, 0)) > 10) {
            Microbot.log("More than 10 tiles from GE, walking to GE");
            Rs2Bank.walkToBank(BankLocation.GRAND_EXCHANGE);
            return false;
        }

        int cycleCount = 0;
        boolean bankHasMoreItems = true;

        while (bankHasMoreItems) {
            cycleCount++;
            Microbot.log("=== STARTING CYCLE " + cycleCount + " ===");

            // Step 1: Open bank and withdraw items not in keep list
            Microbot.log("Step 1: Opening bank and withdrawing non-keep items...");

            if (!Rs2Bank.isOpen()) {
                Microbot.log("Opening bank...");
                Rs2Bank.openBank();
                if (!Rs2Bank.isOpen()) {
                    Microbot.log("Failed to open bank");
                    return false;
                }
                sleep(1000, 1500);
            }

            // Check what's in the bank that we need to withdraw
            Microbot.log("Checking bank contents...");
            List<Rs2ItemModel> bankItems = rs2BankData.getBankItems();

            if (bankItems == null || bankItems.isEmpty()) {
                Microbot.log("No bank items found.");
                Rs2Bank.closeBank();
                bankHasMoreItems = false;
                break;
            }

            // Check what items still need to be processed
            boolean hasItemsToProcess = false;
            List<Integer> itemsToWithdraw = new ArrayList<>();

            for (Rs2ItemModel item : bankItems) {
                if (item == null) continue;

                int itemId = item.getId();
                int qty = item.getQuantity();

                // Skip invalid items
                if (itemId <= 0 || qty <= 0 || itemId == -1) {
                    continue;
                }

                // Check if item is NOT in keep list AND NOT in blacklist
                if (!keepList.contains(itemId) && !untradeableBlacklist.contains(itemId) && !unsellableBlacklist.contains(itemId)) {
                    hasItemsToProcess = true;
                    itemsToWithdraw.add(itemId);
                    Microbot.log("Found item to withdraw: " + itemId + " x" + qty);
                } else if (untradeableBlacklist.contains(itemId)) {
                    Microbot.log("Skipping blacklisted untradeable item: " + itemId);
                } else if (unsellableBlacklist.contains(itemId)) {
                    Microbot.log("Skipping blacklisted unsellable item: " + itemId);
                }
            }

            if (!hasItemsToProcess) {
                Microbot.log("No more items to process in bank, cycle complete!");
                Rs2Bank.closeBank();
                bankHasMoreItems = false;
                break;
            }

            Microbot.log("Found " + itemsToWithdraw.size() + " different sellable items in bank");

            // Withdraw items until inventory is full
            Microbot.log("Withdrawing items to fill inventory...");
            List<Integer> withdrawnItems = new ArrayList<>();

            for (Integer itemId : itemsToWithdraw) {
                Microbot.log("Attempting to withdraw item ID: " + itemId);
                Microbot.log("SETTING TO WITHDRAW NOTES");
                if (!Rs2Bank.hasWithdrawAsNote()) Rs2Bank.setWithdrawAsNote();
                sleep(900,1400);
                if (withdrawAll(itemId)) {
                    withdrawnItems.add(itemId);
                    sleep(300, 800);

                    // If inventory is now full, stop withdrawing and move on to GE
                    if (Rs2Inventory.isFull()) {
                        Microbot.log("Inventory full after withdrawal, moving to GE to sell...");
                        break;
                    }
                } else {
                    Microbot.log("Failed to withdraw item: " + itemId);

                    // If failure was because inventory is full, that's fine — move to GE
                    if (Rs2Inventory.isFull()) {
                        Microbot.log("Inventory is full, moving to GE to sell...");
                        break;
                    }
                }
            }

            Microbot.log("Withdrawal complete. Items withdrawn: " + withdrawnItems.size());

            // Check if we have anything to sell - group by unique item ID
            List<Rs2ItemModel> allInventoryItems = new ArrayList<>();
            Rs2Inventory.items().forEach(allInventoryItems::add);

            // Use a map to track unique items by ID
            Map<Integer, Rs2ItemModel> uniqueItemsMap = new HashMap<>();

            for (Rs2ItemModel item : allInventoryItems) {
                if (item == null) continue;

                int itemId = item.getId();

                // Skip if already processed this item ID
                if (uniqueItemsMap.containsKey(itemId)) {
                    continue;
                }

                if (!isItemInKeepList(item.getId(), keepList)) {
                    if (item.isTradeable()) {
                        uniqueItemsMap.put(itemId, item);
                    } else {
                        // Add untradeable items to blacklist
                        if (untradeableBlacklist.add(itemId)) {
                            Microbot.log("Added to untradeable blacklist: " + item.getName() + " (ID: " + itemId + ")");
                        }
                    }
                }
            }

            List<Rs2ItemModel> inventoryItemsToSell = new ArrayList<>(uniqueItemsMap.values());

            if (inventoryItemsToSell.isEmpty()) {
                Microbot.log("No sellable items in inventory after withdrawal");
                Rs2Bank.closeBank();
                sleep(500, 1000);

                // Clean up any untradeable items before continuing
                cleanupUntradeables(keepList);

                // If we only withdrew untradeables, continue to next cycle
                if (!uniqueItemsMap.isEmpty() || !allInventoryItems.isEmpty()) {
                    Microbot.log("Only untradeable items were withdrawn, continuing to next cycle...");
                    continue;
                }
                break;
            }

            Microbot.log("Found " + inventoryItemsToSell.size() + " UNIQUE items to sell in inventory");

            // Step 2: Close bank and go to Grand Exchange
            Microbot.log("Step 2: Closing bank and opening Grand Exchange...");
            Rs2Bank.closeBank();
            sleep(500, 1000);

            // Open Grand Exchange
            if (!Rs2GrandExchange.isOpen()) {
                Microbot.log("Opening Grand Exchange...");
                Rs2GrandExchange.openExchange();
                sleep(3000, 5000);

                if (!Rs2GrandExchange.isOpen()) {
                    Microbot.log("Failed to open Grand Exchange");
                    return false;
                }
            }

            // Step 3: Sell all items in inventory that are not in keep list
            Microbot.log("Step 3: Selling inventory items...");

            // Before selling, check for any stuck offers and abort them immediately
            abortAnyStuckOffers();

            for (Rs2ItemModel item : inventoryItemsToSell) {
                Microbot.log("=== SELLING: " + item.getName() + " (" + item.getId() + ") ===");

                try {
                    // Double-check it's still sellable and not in keep list
                    if (isItemInKeepList(item.getId(), keepList)) {
                        Microbot.log("SKIP - Item is in keep list: " + item.getName());
                        continue;
                    }

                    if (!item.isTradeable()) {
                        Microbot.log("SKIP - Item not tradeable: " + item.getName());
                        // Add to blacklist if we somehow got here
                        if (untradeableBlacklist.add(item.getId())) {
                            Microbot.log("Added to untradeable blacklist: " + item.getName() + " (ID: " + item.getId() + ")");
                        }
                        continue;
                    }

                    // Skip if in unsellable blacklist
                    if (unsellableBlacklist.contains(item.getId())) {
                        Microbot.log("SKIP - Item is in unsellable blacklist: " + item.getName());
                        continue;
                    }

                    // Check for available slots and collect/abort if needed
                    Microbot.log("Checking available GE slots...");
                    if (Rs2GrandExchange.getAvailableSlot() == null) {
                        Microbot.log("No available slots, checking for stuck offers...");

                        // Check and abort any individual stuck offers
                        abortAnyStuckOffers();
                        sleep(1000, 1500);

                        if (Rs2GrandExchange.getAvailableSlot() == null) {
                            // Still no slots? Collect completed offers
                            Microbot.log("Still no slots, collecting completed offers to bank...");
                            Rs2GrandExchange.collectAllToBank();
                            sleep(1000, 1500);
                        }

                        if (Rs2GrandExchange.getAvailableSlot() == null) {
                            Microbot.log("Still no available slots after collecting/aborting");
                            continue;
                        }
                    }

                    String itemName = item.getName();
                    int itemCount = Rs2Inventory.count(itemName, true);
                    Microbot.log("Selling " + itemCount + " x " + itemName);

                    // Track when this offer starts
                    long startTime = System.currentTimeMillis();
                    offerStartTimes.put(item.getId(), startTime);

                    // Attempt to sell
                    boolean success = attemptSellWithTimeout(itemName, percent, true);
                    Microbot.log("Sell result for " + itemName + ": " + success);

                    if (success) {
                        // Verify items were sold
                        sleep(1000);
                        int remainingCount = Rs2Inventory.count(itemName, true);
                        if (remainingCount > 0) {
                            Microbot.log("WARNING: " + remainingCount + " items still in inventory after sell");
                        } else {
                            Microbot.log("SUCCESS: All " + itemName + " sold");
                        }
                    } else {
                        Microbot.log("FAILED to sell: " + itemName);
                    }

                    sleep(1200, 2000);

                } catch (Exception e) {
                    Microbot.log("Exception selling " + item.getName() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // Step 4: Close Grand Exchange and check if we need another cycle
            Microbot.log("Step 4: Closing Grand Exchange and checking for more items...");
            Rs2GrandExchange.closeExchange();
            sleep(1000, 1500);

            // Clean up any untradeable items that might still be in inventory
            cleanupUntradeables(keepList);

            // Check if bank still has sellable items for next cycle
            Microbot.log("Checking if bank has more sellable items for next cycle...");

            if (cycleCount >= 20) { // Safety limit
                Microbot.log("Reached maximum cycle limit (20), stopping");
                bankHasMoreItems = false;
            }

            Microbot.log("=== COMPLETED CYCLE " + cycleCount + " ===");
            sleep(1000, 2000);
        }

        Microbot.log("=== SELL ALL EXCEPT KEEP LIST CYCLING COMPLETE ===");
        Microbot.log("Total cycles completed: " + cycleCount);
        Microbot.log("Total items in untradeable blacklist: " + untradeableBlacklist.size());

        // Final check - verify no sellable items remain in inventory
        List<Rs2ItemModel> allFinalInventoryItems = new ArrayList<>();
        Rs2Inventory.items().forEach(allFinalInventoryItems::add);

        // Group by unique item ID to avoid counting duplicates
        Map<Integer, Rs2ItemModel> uniqueFinalItems = new HashMap<>();
        for (Rs2ItemModel item : allFinalInventoryItems) {
            if (item != null && !keepList.contains(item.getId()) && item.isTradeable()) {
                uniqueFinalItems.put(item.getId(), item);
            }
        }

        if (!uniqueFinalItems.isEmpty()) {
            Microbot.log("WARNING: Still have sellable items in inventory:");
            uniqueFinalItems.values().forEach(item -> {
                int count = Rs2Inventory.count(item.getName(), true);
                Microbot.log("  Remaining: " + item.getName() + " (ID: " + item.getId() + ") x" + count);
            });
            return false;
        }

        Microbot.log("SUCCESS: All sellable items have been processed");
        return true;
    }

    // Add this helper method at the class level
    private static boolean isItemInKeepList(int inventoryItemId, List<Integer> keepList) {
        // Check if the actual cert ID is in keepList (unlikely but safe)
        if (keepList.contains(inventoryItemId)) {
            return true;
        }

        // Check if the BASE version (cert - 1) is in keepList
        // This handles the case where we have cert in inventory but keepList has base ID
        return keepList.contains(inventoryItemId - 1);
    }

    // Optional: Method to clear the blacklist if needed
    public static void clearUntradeableBlacklist() {
        untradeableBlacklist.clear();
        Microbot.log("Untradeable blacklist cleared");
    }

    public static void clearUnsellableBlacklist() {
        unsellableBlacklist.clear();
        Microbot.log("Unsellable blacklist cleared");
    }

    // Optional: Method to view blacklist
    public static Set<Integer> getUntradeableBlacklist() {
        return new HashSet<>(untradeableBlacklist);
    }

    public static Set<Integer> getUnsellableBlacklist() {
        return new HashSet<>(unsellableBlacklist);
    }

    // Helper method to abort ANY stuck offers (not just when all are stuck)
    private static void abortAnyStuckOffers() {
        Microbot.log("=== CHECKING AND ABORTING ANY STUCK OFFERS ===");

        GrandExchangeOffer[] offers = Microbot.getClient().getGrandExchangeOffers();
        List<Pair<Integer, Integer>> offersToAbort = new ArrayList<>(); // Pair of (slotIndex, itemId)

        for (int i = 0; i < offers.length; i++) {
            GrandExchangeOffer offer = offers[i];

            if (offer.getState() != GrandExchangeOfferState.SELLING) {
                continue; // Only check SELLING offers
            }

            int itemId = offer.getItemId();

            if (!offerStartTimes.containsKey(itemId)) {
                // Start tracking this offer
                offerStartTimes.put(itemId, System.currentTimeMillis());
                Microbot.log("Started tracking offer for item ID " + itemId + " in slot " + i);
                continue;
            }

            long elapsedTime = System.currentTimeMillis() - offerStartTimes.get(itemId);

            if (elapsedTime >= OFFER_TIMEOUT_MS) {
                Microbot.log("Found stuck offer for item ID " + itemId + " in slot " + i + " (stuck for " + (elapsedTime / 1000) + " seconds)");
                offersToAbort.add(Pair.of(i, itemId));
            }
        }

        // Abort each stuck offer individually
        if (!offersToAbort.isEmpty()) {
            Microbot.log("Aborting " + offersToAbort.size() + " stuck offer(s)...");

            for (Pair<Integer, Integer> offerInfo : offersToAbort) {
                int slotIndex = offerInfo.getLeft();
                int itemId = offerInfo.getRight();

                try {
                    // Get the slot enum from index
                    GrandExchangeSlots slot = GrandExchangeSlots.values()[slotIndex];

                    Microbot.log("Aborting offer in slot " + slot + " for item ID " + itemId);

                    // Abort this specific offer
                    Widget slotWidget = GrandExchangeWidget.getSlot(slot);
                    if (slotWidget != null) {
                        NewMenuEntry menuEntry = new NewMenuEntry("Abort offer", "", 2, MenuAction.CC_OP, 2, slotWidget.getId(), false);
                        Rectangle bounds = slotWidget.getBounds() != null && Rs2UiHelper.isRectangleWithinCanvas(slotWidget.getBounds())
                                ? slotWidget.getBounds()
                                : Rs2UiHelper.getDefaultRectangle();
                        Microbot.doInvoke(menuEntry, bounds);

                        sleep(600, 900);

                        // Collect the aborted items to bank
                        Rs2GrandExchange.collectAllToBank();
                        sleep(600, 900);
                    }

                    // Blacklist this item
                    if (unsellableBlacklist.add(itemId)) {
                        Microbot.log("Added item ID " + itemId + " to unsellable blacklist");
                    }

                    // Remove from tracking
                    offerStartTimes.remove(itemId);

                } catch (Exception e) {
                    Microbot.log("Error aborting offer in slot " + slotIndex + ": " + e.getMessage());
                }
            }

            Microbot.log("Finished aborting stuck offers");
        } else {
            Microbot.log("No stuck offers found");
        }
    }

    // Helper method to drop/destroy untradeable items in inventory
    private static void cleanupUntradeables(List<Integer> keepList) {
        Microbot.log("=== CLEANUP: Checking for untradeable items to drop/destroy ===");

        List<Rs2ItemModel> allInventoryItems = new ArrayList<>();
        Rs2Inventory.items().forEach(allInventoryItems::add);

        List<Rs2ItemModel> untradeablesToRemove = new ArrayList<>();

        for (Rs2ItemModel item : allInventoryItems) {
            if (item != null && !keepList.contains(item.getId()) && !item.isTradeable()) {
                untradeablesToRemove.add(item);
                // Add to blacklist if not already there
                if (untradeableBlacklist.add(item.getId())) {
                    Microbot.log("Added to untradeable blacklist: " + item.getName() + " (ID: " + item.getId() + ")");
                }
            }
        }

        if (untradeablesToRemove.isEmpty()) {
            Microbot.log("No untradeable items to remove");
            return;
        }

        Microbot.log("Found " + untradeablesToRemove.size() + " untradeable items to remove");

        for (Rs2ItemModel item : untradeablesToRemove) {
            try {
                String itemName = item.getName();
                Microbot.log("Attempting to remove: " + itemName + " (ID: " + item.getId() + ")");

                // Try to drop the item first
                boolean dropped = Rs2Inventory.drop(itemName);

                if (dropped) {
                    Microbot.log("Successfully dropped: " + itemName);
                    sleep(600, 900);

                    // Verify it's gone
                    if (!Rs2Inventory.contains(itemName)) {
                        Microbot.log("Confirmed removed: " + itemName);
                        continue;
                    }
                }

                // If drop failed or item still in inventory, try destroy
                Microbot.log("Attempting to destroy: " + itemName);

                // Try destroy action multiple times (some items need multiple attempts)
                int destroyAttempts = 0;
                while (Rs2Inventory.contains(itemName) && destroyAttempts < 3) {
                    destroyAttempts++;
                    Microbot.log("Destroy attempt " + destroyAttempts + " for: " + itemName);

                    Rs2Inventory.interact(itemName, "Destroy");
                    sleep(1000, 1500);

                    // Check if item is gone
                    if (!Rs2Inventory.contains(itemName)) {
                        Microbot.log("Successfully destroyed: " + itemName);
                        break;
                    }
                }

                // Final check
                if (Rs2Inventory.contains(itemName)) {
                    Microbot.log("WARNING: Failed to remove " + itemName + " after all attempts");
                }

            } catch (Exception e) {
                Microbot.log("Exception removing " + item.getName() + ": " + e.getMessage());
            }
        }

        Microbot.log("=== CLEANUP COMPLETE ===");
    }

    public static List<Integer> withdrawAllExcept(List<Integer> keepItems) {
        Microbot.log("Started withdrawAllExcept");

        List<Integer> allProcessedItems = new ArrayList<>();
        int maxCycles = 10; // Safety limit to prevent infinite loops
        int currentCycle = 0;

        while (currentCycle < maxCycles) {
            currentCycle++;
            Microbot.log("Starting cycle " + currentCycle);

            if (!isOpen()) {
                openBank();
                sleepUntil(Rs2Bank::isOpen, 3000);
            }

            List<Integer> withdrawnItems = new ArrayList<>();
            List<Integer> itemsToSell = new ArrayList<>();
            List<Rs2ItemModel> bankItems = rs2BankData.getBankItems();

            if (bankItems == null || bankItems.isEmpty()) {
                Microbot.log("No bank items found.");
                break;
            }

            // Check what items still need to be processed
            boolean hasItemsToProcess = false;
            for (Rs2ItemModel item : bankItems) {
                if (item == null) continue;

                int itemId = item.getId();
                int qty = item.getQuantity();

                // Skip invalid items: empty slots, placeholders, or zero quantities
                if (itemId <= 0 || qty <= 0) {
                    continue;
                }

                // Additional safety check for empty bank slots
                if (itemId == -1) {
                    continue;
                }

                if (!keepItems.contains(itemId)) {
                    hasItemsToProcess = true;
                    itemsToSell.add(itemId);
                    Microbot.log("Found item to process: " + itemId + " x" + qty);

                    if (withdrawAll(itemId)) {
                        withdrawnItems.add(itemId);
                        sleep(300, 800);
                    } else {
                        Microbot.log("Failed to withdraw item (inventory might be full): " + itemId);
                    }
                }
            }

            // If no more items to process, we're done
            if (!hasItemsToProcess) {
                Microbot.log("No more items to process in bank. Finished!");
                break;
            }

            Microbot.log("Successfully withdrawn this cycle: " + withdrawnItems);
            Microbot.log("Items that should be sold this cycle: " + itemsToSell);
            allProcessedItems.addAll(itemsToSell);

            // If we have items to sell (either withdrawn or already in inventory)
            if (!itemsToSell.isEmpty()) {
                sleep(1999, 4400);
                closeBank();
                sleep(2000, 5000);

                Rs2GrandExchange.openExchange();
                sleep(1900, 2900);
                Rs2GrandExchange.collectAllToBank1();

                sellLoot(itemsToSell, -99);

                sleep(2000, 4000); // Wait for selling to complete

                // Check if there are still items in bank to process
                // If so, continue the loop to go back to bank
                Microbot.log("Completed selling cycle " + currentCycle + ". Checking for more items...");
            } else {
                Microbot.log("No items to sell this cycle.");
                break;
            }
        }

        if (currentCycle >= maxCycles) {
            Microbot.log("Warning: Reached maximum cycles limit. Some items may remain.");
        }

        Microbot.log("Total items processed: " + allProcessedItems);
        Microbot.log("Finished withdrawAllExcept after " + currentCycle + " cycles");

        return allProcessedItems;
    }




    /**
     * Collects items from the collection box and deposits them into the bank.
     *
     * @return true if the operation is successfully initiated and completes processing.
     */

    public static boolean bankAllCollectionBoxItems() {
        openCollectionBox();
        sleepUntil(Rs2Bank::collectionBoxIsOpen, 5000);
        Rs2Widget.clickWidget(26345476);
        return true;
    }
    /**
     * Collects items into the inventory by interacting with the collection box
     * and selecting the inventory option.
     *
     * @return true if the operation to collect items into the inventory is successfully initiated
     */
    public static boolean inventoryAllCollectionBoxItems() {
        openCollectionBox();
        sleepUntil(Rs2Bank::collectionBoxIsOpen, 5000);
        Rs2Widget.clickWidget(26345475);
        return true;
    }

    /**
     * Closes the collection box interface.
     */
    public static boolean closeCollectionBox() {
        final Widget collectionBoxParent = Rs2Widget.getWidget(402,2);
        if (collectionBoxParent == null) return true;

        final Widget[] closeWidget = collectionBoxParent.getDynamicChildren();
        if (closeWidget == null) return false; // Shouldn't happen parent but no children?

        Rs2Widget.clickWidget(closeWidget[3]);
        return sleepUntil(() -> !collectionBoxIsOpen(), 5000);
    }

    /**
     * Determines if the collection box is visible in the user interface based on widget text.
     *
     * @return true if the collection box widget with the specified text is visible; false otherwise.
     */
    public static boolean isCollectionBoxVisible() {
        return Rs2Widget.hasWidgetText("Collection box", 402, 2, false);
    }

    /**
     * Checks if the collection box is currently open and visible.
     *
     * @return true if the collection box is visible, false otherwise.
     */
    public static boolean collectionBoxIsOpen() {
        return isCollectionBoxVisible();
    }


    public static boolean openBank(Rs2NpcModel npc) {
        Microbot.status = "Opening bank";
        try {
            if (isOpen()) return true;
            if (Rs2Inventory.isItemSelected()) Microbot.getMouse().click();

            if (npc == null) return false;

            boolean interactResult = Rs2Npc.interact(npc, "bank");

            if (!interactResult) {
                return false;
            }

            sleepUntil(Rs2Bank::isOpen);
            sleep(Rs2Random.randomGaussian(800,200));
            return true;
        } catch (Exception ex) {
            Microbot.logStackTrace("Rs2Bank", ex);
        }
        return false;
    }

    public static boolean openBank(NPC npc) {
        return openBank(new Rs2NpcModel(npc));
    }

    /**
     * open bank identified by tile object.
     *
     * @param object TileObject
     *
     * @return true if bank is open
     */
    public static boolean openBank(TileObject object) {
        Microbot.status = "Opening bank";
        try {
            if (isOpen()) return true;
            if (Rs2Inventory.isItemSelected()) Microbot.getMouse().click();

            if (object == null) return false;

            boolean interactResult = Rs2GameObject.interact(object, "bank");

            if (!interactResult) {
                return false;
            }

            sleepUntil(Rs2Bank::isOpen);
            sleep(Rs2Random.randomGaussian(800,200));
            return true;
        } catch (Exception ex) {
            Microbot.logStackTrace("Rs2Bank", ex);
        }
        return false;
    }

    /**
     * Sets the values of the inventoryWidget
     *
     * @param id item id
     */
    private static boolean handleWearItem(int id) {
        Rs2ItemModel rs2Item = Rs2Inventory.get(id);
        if (rs2Item == null) return false;
        container = BANK_INVENTORY_ITEM_CONTAINER;

        invokeMenu(9, rs2Item);
        return true;
    }

    /**
     * Tries to wear an item identified by its id.
     *
     * @param id item id
     */
    public static boolean wearItem(int id) {
        return handleWearItem(id);
    }

    /**
     * find an item in the bank identified by its id.
     *
     * @param id item id to find
     *
     * @return bankItem
     */
    @SuppressWarnings("UnnecessaryLocalVariable")
    private static Rs2ItemModel findBankItem(int id) {
        List<Rs2ItemModel> bankItems = rs2BankData.getBankItems();
        if (bankItems == null) return null;
        if (bankItems.stream().findAny().isEmpty()) return null;

        Rs2ItemModel bankItem = bankItems.stream().filter(x -> x.getId() == id).findFirst().orElse(null);

        return bankItem;
    }

    /**
     * Finds an item in the bank based on its name.
     *
     * @param name  The name of the item.
     * @param exact If true, requires an exact name match.
     *
     * @return The item widget, or null if the item isn't found.
     */
    @SuppressWarnings("UnnecessaryLocalVariable")
    private static Rs2ItemModel findBankItem(String name, boolean exact) {
        return findBankItem(name, exact, 1);
    }

    /**
     * Finds an item in the bank based on its name.
     *
     * @param name   The name of the item.
     * @param exact  If true, requires an exact name match.
     * @param amount the amount needed to find in the bank
     *
     * @return The item widget, or null if the item isn't found.
     */
    @SuppressWarnings("UnnecessaryLocalVariable")
    private static Rs2ItemModel findBankItem(String name, boolean exact, int amount) {
    List<Rs2ItemModel> bankItems = rs2BankData.getBankItems();
    if (bankItems == null || bankItems.isEmpty()) {
        return null;
    }
    final String lowerCaseName = name.toLowerCase();
    return bankItems.stream()
            .filter(x -> exact ? x.getName().equalsIgnoreCase(lowerCaseName) : x.getName().toLowerCase().contains(lowerCaseName))
            .filter(x -> x.getQuantity() >= amount)
            .findAny()
            .orElse(null);
}

    /**
     * Finds an item in the bank based on a list of names.
     *
     * @param names  A list of potential item names.
     * @param exact  If true, requires an exact name match.
     * @param amount The minimum amount needed to find in the bank.
     * @return The first matching item widget, or null if no matching item is found.
     */
    private static Rs2ItemModel findBankItem(List<String> names, boolean exact, int amount) {
        List<Rs2ItemModel> bankItems = rs2BankData.getBankItems();
        if (bankItems == null || bankItems.isEmpty()) return null;

        return bankItems.stream()
                .filter(item -> names.stream().anyMatch(name -> exact
                        ? item.getName().equalsIgnoreCase(name)
                        : item.getName().toLowerCase().contains(name.toLowerCase())))
                .filter(item -> item.getQuantity() >= amount)
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns the nearest accessible bank to the local player’s current location.
     *
     * @return the nearest {@link BankLocation}, or {@code null} if none was reachable
     */
    public static BankLocation getNearestBank() {
        return getNearestBank(Microbot.getClient().getLocalPlayer().getWorldLocation());
    }

    /**
     * Returns the nearest accessible bank to the specified world point,
     * using a default search radius of 15 tiles.
     *
     * @param worldPoint the starting location from which to search for banks
     * @return the nearest {@link BankLocation}, or {@code null} if none was reachable
     */
    public static BankLocation getNearestBank(WorldPoint worldPoint) {
        return getNearestBank(worldPoint, 20);
    }

    /**
     * Finds the nearest accessible bank location from the given world point.
     * <p>
     * First, searches for bank booth {@link TileObject}s within
     * {@code maxObjectSearchRadius} tiles of the player and picks the closest
     * one whose underlying {@link BankLocation#hasRequirements()} passes. If no booth
     * is found or none are within range, falls back to running a full pathfinding
     * search (including configured transports) to all accessible bank coordinates,
     * then returns the bank at the end of the shortest path.
     * </p>
     *
     * @param worldPoint            the starting location for pathfinding
     * @param maxObjectSearchRadius the maximum radius (in tiles) to scan for bank booth objects
     * @return the nearest {@link BankLocation}, or {@code null} if no accessible bank could be reached
     */
    public static BankLocation getNearestBank(WorldPoint worldPoint, int maxObjectSearchRadius) {
        AbstractMap.SimpleEntry<List<WorldPoint>, BankLocation> result = getPathAndBankToNearestBank(worldPoint, maxObjectSearchRadius);
        return result != null ? result.getValue() : null;
    }

    /**
     * Private helper method that finds both the path and bank location to the nearest accessible bank.
     *
     * @param worldPoint            the starting location for pathfinding
     * @param maxObjectSearchRadius the maximum radius (in tiles) to scan for bank booth objects
     * @return A SimpleEntry containing the path (key) and bank location (value), or null if no accessible bank could be reached
     */
    private static AbstractMap.SimpleEntry<List<WorldPoint>, BankLocation> getPathAndBankToNearestBank(WorldPoint worldPoint, int maxObjectSearchRadius) {
        Microbot.log("Finding nearest bank...");

        Set<BankLocation> allBanks = Arrays.stream(BankLocation.values())
                .collect(Collectors.toSet());
        if (Objects.equals(Microbot.getClient().getLocalPlayer().getWorldLocation(), worldPoint)) {
            List<TileObject> bankObjs = Stream.concat(
                            Stream.of(Rs2GameObject.findBank(maxObjectSearchRadius)),
                            Stream.of(Rs2GameObject.findGrandExchangeBooth(maxObjectSearchRadius))
                    )
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            Optional<BankLocation> byObject = bankObjs.stream()
                    .map(obj -> {
                        BankLocation closestBank = allBanks.stream()
                                .min(Comparator.comparingInt(b -> Rs2WorldPoint.quickDistance(obj.getWorldLocation(), b.getWorldPoint())))
                                .orElse(null);

                        int dist = obj.getWorldLocation().distanceTo(closestBank.getWorldPoint());

                        return new AbstractMap.SimpleEntry<>(closestBank, dist);
                    })
                    .filter(e -> e.getKey() != null && e.getValue() <= maxObjectSearchRadius)
                    .min(Comparator.comparingInt(Map.Entry::getValue))
                    .map(Map.Entry::getKey);
            if (byObject.isPresent() && byObject.get().hasRequirements()) {
                Microbot.log("Found nearest bank (object): " + byObject.get());
                BankLocation returnBankLocation = byObject.get();
                List<WorldPoint> path = new ArrayList<>(Collections.singletonList(byObject.get().getWorldPoint()));
                return new AbstractMap.SimpleEntry<>(path, returnBankLocation);
            }
        }

        // Measure accessible banks filtering performance, expensive operation takes up to 2500 ms
        long accessibleBanksStart = System.nanoTime();
        Set<BankLocation> accessibleBanks = allBanks.stream()
                .filter(BankLocation::hasRequirements)
                .collect(Collectors.toSet());
        long accessibleBanksTime = System.nanoTime() - accessibleBanksStart;
        log.info("Accessible banks filtering performance: {}ms, Found {} accessible banks out of {} total",
                accessibleBanksTime / 1_000_000.0, accessibleBanks.size(), BankLocation.values().length);

        if (accessibleBanks.isEmpty()) {
            Microbot.log("No accessible banks found");
            return null;
        }
        Set<WorldPoint> targets = accessibleBanks.stream()
                .map(BankLocation::getWorldPoint)
                .collect(Collectors.toSet());

        if (ShortestPathPlugin.getPathfinderConfig().getTransports().isEmpty()) {
            ShortestPathPlugin.getPathfinderConfig().refresh();
        }

        List<WorldPoint> targetsList = targets.stream()
                .collect(Collectors.toList());

        long originalStart = System.nanoTime();
        Pathfinder pf = new Pathfinder(ShortestPathPlugin.getPathfinderConfig(), worldPoint, targets);
        pf.run();
        List<WorldPoint> path = pf.getPath();
        long originalTime = System.nanoTime() - originalStart;

        if (path.isEmpty()) {
            Microbot.log("Unable to find path to nearest bank");
            return null;
        }

        // FIX START: Explicitly use Microbot WorldArea via primitives
        WorldPoint nearestTile = path.get(path.size() - 1);

        net.runelite.client.plugins.microbot.util.api.coords.WorldArea nearestTileArea =
                new net.runelite.client.plugins.microbot.util.api.coords.WorldArea(
                        nearestTile.getX(), nearestTile.getY(), 2, 2, nearestTile.getPlane());

        Optional<BankLocation> byPath = accessibleBanks.stream()
                .filter(b -> {
                    net.runelite.client.plugins.microbot.util.api.coords.WorldArea accessibleBankArea =
                            new net.runelite.client.plugins.microbot.util.api.coords.WorldArea(
                                    b.getWorldPoint().getX(), b.getWorldPoint().getY(), 2, 2, b.getWorldPoint().getPlane());

                    return accessibleBankArea.intersectsWith2D(nearestTileArea);
                })
                .findFirst();
        // FIX END

        BankLocation returnBankLocation = null;
        if (byPath.isPresent()) {
            Microbot.log("Found nearest bank (shortest path): " + byPath.get());
            returnBankLocation = byPath.get();
        } else {
            Microbot.log("Nearest bank point " + nearestTile + " did not match any BankLocation");
        }
        return new AbstractMap.SimpleEntry<>(path, returnBankLocation);
    }

    /**
     * Finds the path to the nearest accessible bank location from the given world point.
     * Uses a default search radius of 50 tiles for bank object scanning.
     *
     * @param worldPoint the starting location for pathfinding
     * @return the complete path to the nearest bank as List<WorldPoint>, or empty list if no accessible bank could be reached
     */
    public static List<WorldPoint> getPathToNearestBank(WorldPoint worldPoint) {
        return getPathToNearestBank(worldPoint, 50);
    }

    /**
     * Finds the path to the nearest accessible bank location from the player's current location.
     * Uses a default search radius of 50 tiles for bank object scanning.
     *
     * @return the complete path to the nearest bank as List<WorldPoint>, or empty list if no accessible bank could be reached
     */
    public static List<WorldPoint> getPathToNearestBank() {
        return getPathToNearestBank(Rs2Player.getWorldLocation(), 50);
    }

    /**
     * Finds the path to the nearest accessible bank location from the given world point.
     * <p>
     * Uses the same logic as getNearestBank but returns the complete path instead of the BankLocation.
     * First, searches for bank booth {@link TileObject}s within
     * {@code maxObjectSearchRadius} tiles of the player and picks the closest
     * one whose underlying {@link BankLocation#hasRequirements()} passes. If no booth
     * is found or none are within range, falls back to running a full pathfinding
     * search (including configured transports) to all accessible bank coordinates,
     * then returns the complete path to the nearest bank.
     * </p>
     *
     * @param worldPoint            the starting location for pathfinding
     * @param maxObjectSearchRadius the maximum radius (in tiles) to scan for bank booth objects
     * @return the complete path to the nearest bank as List<WorldPoint>, or empty list if no accessible bank could be reached
     */
    public static List<WorldPoint> getPathToNearestBank(WorldPoint worldPoint, int maxObjectSearchRadius) {
        AbstractMap.SimpleEntry<List<WorldPoint>, BankLocation> result = getPathAndBankToNearestBank(worldPoint, maxObjectSearchRadius);
        return result != null ? result.getKey() : new ArrayList<>();
    }

    /**
     * Walks to the closest bank using the nearest bank location.
     * Toggles run energy if the player is not already running.
     *
     * @return true if the player's location is within 4 tiles of the bank location.
     */
    public static boolean walkToBank() {
        return walkToBank(getNearestBank());
    }

    /**
     * Walks to a specified bank location.
     * Toggles run energy if the player is not already running.
     *
     * @param bankLocation the target bank location to walk to.
     * @return true if the player's location is within 4 tiles of the specified bank location.
     */
    public static boolean walkToBank1(BankLocation bankLocation) {
        return walkToBank1(bankLocation, true);
    }

    /**
     * Walks to a specified bank location.
     * Toggles run energy if the player is not already running.
     *
     * @param bankLocation the target bank location to walk to.
     * @return true if the player's location is within 4 tiles of the specified bank location.
     */
    public static boolean walkToBank(BankLocation bankLocation) {
        return walkToBank(bankLocation, true);
    }

    /**
     * Walks to a specified bank location with an option to toggle run energy.
     * If the bank is already open, the method exits immediately.
     *
     * @param bankLocation the target bank location to walk to.
     * @param toggleRun    whether to toggle run energy during the walk.
     * @return true if the player's location is within 4 tiles of the specified bank location.
     */
    public static boolean walkToBank1(BankLocation bankLocation, boolean toggleRun) {
        Rs2Player.toggleRunEnergy(toggleRun);
        Microbot.log("in walk to bank1 just about to call walkTo");
        Microbot.status = "Walking to nearest bank " + bankLocation.toString();
        if (!ShortestPathPlugin.isStartPointSet()) {Rs2Walker.walkTo(bankLocation.getWorldPoint(), 4);}
        return bankLocation.getWorldPoint().distanceTo2D(Microbot.getClient().getLocalPlayer().getWorldLocation()) <= 4;
    }

    public static boolean walkToBank1() {
        return walkToBank1(getNearestBank());
    }

    /**
     * Walks to a specified bank location with an option to toggle run energy.
     * If the bank is already open, the method exits immediately.
     *
     * @param bankLocation the target bank location to walk to.
     * @param toggleRun    whether to toggle run energy during the walk.
     * @return true if the player's location is within 4 tiles of the specified bank location.
     */
    public static boolean walkToBank(BankLocation bankLocation, boolean toggleRun) {
        if (Rs2Bank.isOpen()) return true;
        Rs2Player.toggleRunEnergy(toggleRun);
        Microbot.status = "Walking to nearest bank " + bankLocation.toString();
        Rs2Walker.walkTo(bankLocation.getWorldPoint(), 4);
        return bankLocation.getWorldPoint().distanceTo2D(Microbot.getClient().getLocalPlayer().getWorldLocation()) <= 4;
    }

    /**
     * Distance from the nearest bank location
     *
     * @param distance
     * @return true if player location is less than distance away from the bank location
     */
    public static boolean isNearBank(int distance) {
        return isNearBank(getNearestBank(), distance);
    }

    /**
     * Distance from bank location
     *
     * @param bankLocation
     * @param distance
     * @return true if player location is less than distance away from the bank location
     */
    public static boolean isNearBank(BankLocation bankLocation, int distance) {
        int distanceToBank = Rs2Walker.getDistanceBetween(Microbot.getClient().getLocalPlayer().getWorldLocation(), bankLocation.getWorldPoint());
        return distanceToBank <= distance;
    }

    /**
     * Walks to the closest bank and attempts to use the bank interface.
     * Toggles run energy if the player is not already running.
     *
     * @return true if the bank interface is successfully opened.
     */
    public static boolean walkToBankAndUseBank() {
        return walkToBankAndUseBank(getNearestBank());
    }

    /**
     * Walks to a specified bank location and attempts to use the bank interface.
     * Toggles run energy if the player is not already running.
     *
     * @param bankLocation the target bank location to walk to and use.
     * @return true if the bank interface is successfully opened.
     */
    public static boolean walkToBankAndUseBank(BankLocation bankLocation) {
        return walkToBankAndUseBank(bankLocation, true);
    }

    /**
     * Walks to a specified bank location with an option to toggle run energy and attempts to use the bank interface.
     * If the bank is already open, the method exits immediately.
     *
     * @param bankLocation the target bank location to walk to and use.
     * @param toggleRun    whether to toggle run energy during the walk.
     * @return true if the bank interface is successfully opened.
     */
    public static boolean walkToBankAndUseBank(BankLocation bankLocation, boolean toggleRun) {
        if (Rs2Bank.isOpen()) return true;
        Rs2Player.toggleRunEnergy(toggleRun);
        Microbot.status = "Walking to nearest bank " + bankLocation.toString();
        boolean result = Rs2Walker.getDistanceBetween(Microbot.getClient().getLocalPlayer().getWorldLocation(), bankLocation.getWorldPoint()) <= 8;
        if (!result) {
            Rs2Walker.walkTo(bankLocation.getWorldPoint());
        }
        return Rs2Bank.openBank();
    }

    /**
     * Use bank or chest
     *
     * @return true if bank is opened
     */
    @Deprecated(since="Use openBank")
    public static boolean useBank() {
        return openBank();
    }

    /**
     * Updates the bank items in memory based on the provided event.
     *
     * @param e The event containing the latest bank items.
     */
    public static void updateLocalBank(ItemContainerChanged e) {
        List<Rs2ItemModel> list = updateItemContainer(InventoryID.BANK.getId(), e);
        if (list != null) {
            // Update the centralized bank data
            rs2BankData.set(list);
        }
    }

     
    /**
     * Updates the cached bank data with the latest bank items and saves to config.
     * 
     * @param items The current bank items
     */
    private static void updateBankCache(List<Rs2ItemModel> items) {
        if (items != null) {
            rs2BankData.set(items);
            saveBankToConfig();
        }
    }

    /**
     * Loads the initial bank state from config. Should be called when a player logs in.
     * Similar to QuestBankManager.loadInitialStateFromConfig().
     */
    public static void loadInitialBankStateFromConfig() {
        if (!loggedInStateKnown) {
            Player localPlayer = Microbot.getClient().getLocalPlayer();
            if (localPlayer != null && localPlayer.getName() != null) {
                loggedInStateKnown = true;
                loadState();
            }
        }
    }

    /**
     * Sets the initial state as unknown. Called when logging out or changing profiles.
     */
    public static void setUnknownInitialBankState() {
        loggedInStateKnown = false;
    }

    /**
     * Loads bank state from config, handling profile changes.
     * Similar to QuestBank.loadState().
     */
    public static void loadState() {
        // Only re-load from config if loading from a new profile
        if (!RuneScapeProfileType.getCurrent(Microbot.getClient()).equals(worldType)) {
            // If we've hopped between profiles, save current state first
            if (rsProfileKey != null) {
                saveBankToConfig();
            }
            loadBankFromConfig();
        }
    }

    /**
     * Loads bank data from RuneLite config system.
     * Similar to QuestBank.loadBankFromConfig().
     */
    private static void loadBankFromConfig() {
        rsProfileKey = Microbot.getConfigManager().getRSProfileKey();
        worldType = RuneScapeProfileType.getCurrent(Microbot.getClient());

        String json = Microbot.getConfigManager().getRSProfileConfiguration(CONFIG_GROUP, BANK_KEY);
        try {
            if (json != null && !json.isEmpty()) {
                int[] data = gson.fromJson(json, int[].class);
                rs2BankData.setIdQuantityAndSlot(data);
                
                // Load cached items if no live bank data
                if (rs2BankData.getBankItems().isEmpty()) {
                    // Cache is already loaded via setIdQuantityAndSlot
                    log.info("Loaded {} cached bank items from config", rs2BankData.size());
                }
            } else {
                rs2BankData.setEmpty();
                log.debug("No cached bank data found in config");
            }
        } catch (JsonSyntaxException err) {
            log.warn("Failed to parse cached bank data from config, resetting cache", err);
            rs2BankData.setEmpty();
            saveBankToConfig();
        }
    }

    /**
     * Saves the current bank state to RuneLite config system.
     * Similar to QuestBank.saveBankToConfig().
     */
    public static void saveBankToConfig() {
        if (rsProfileKey == null || Microbot.getConfigManager() == null) {
            return;
        }

        try {
            String json = gson.toJson(rs2BankData.getIdQuantityAndSlot());
            Microbot.getConfigManager().setConfiguration(CONFIG_GROUP, rsProfileKey, BANK_KEY, json);
            log.debug("Saved {} bank items to config cache", rs2BankData.size());
        } catch (Exception e) {
            log.error("Failed to save bank data to config", e);
        }
    }

    /**
     * Clears the bank cache state. Called when logging out.
     */
    public static void emptyBankState() {
        rsProfileKey = null;
        worldType = null;
        rs2BankData.setEmpty();
        loggedInStateKnown = false;
        log.debug("Emptied bank state and cache");
    }
   

    /**
     * Checks if we have cached bank data available.
     * 
     * @return true if cached bank data is available, false otherwise
     */
    public static boolean hasCachedBankData() {
        return !rs2BankData.isEmpty();
    }

    /**
     * Handle bank pin boolean.
     *
     * @param pin the pin
     *
     * @return the boolean
     */
    public static boolean handleBankPin(String pin) {
        if (!isBankPinWidgetVisible()) return true;

        final String[] DIGIT_INSTRUCTIONS = {"FIRST digit", "SECOND digit", "THIRD digit", "FOURTH digit"};
        if (pin == null || pin.length() != DIGIT_INSTRUCTIONS.length || !pin.matches("\\d+")) {
            Microbot.log("Unable to enter bankpin with value " + pin);
            return false;
        }

		synchronized(Rs2Bank.class) {
            if (!isBankPinWidgetVisible()) return true;
            for (int i = 0; i < pin.length(); i++) {
                final char c = pin.charAt(i);
                final String expectedInstruction = DIGIT_INSTRUCTIONS[i];

                boolean instructionVisible = sleepUntil(() -> Rs2Widget.hasWidgetText(expectedInstruction, 213, 10, false), 2000);
                if (!instructionVisible) {
                    Microbot.log("Failed to detect instruction within timeout period: " + expectedInstruction);
                    return false;
                }
/// /bank plugin is not present in iNfInITY
                //if (isBankPluginEnabled() && hasKeyboardBankPinEnabled()) {
                    //Rs2Keyboard.typeString(String.valueOf(c));
               // } else {
                  //  Rs2Widget.clickWidget(String.valueOf(c), Optional.of(213), 0, true);
               // }
            }
            return true;
		}
    }

    public static boolean handleBankPin() {
        final String encryptedBankPin = Login.activeProfile.getBankPin();
        if (encryptedBankPin == null || encryptedBankPin.isBlank() || encryptedBankPin.equalsIgnoreCase("**bankpin**"))
            return !isBankPinWidgetVisible();
        try {
            return handleBankPin(Encryption.decrypt(Login.activeProfile.getBankPin()));
        } catch (Exception ex) {
            log.error("Error handling Bank Pin", ex);
            return false;
        }
    }

    public static boolean isBankPinWidgetVisible() {
        return Rs2Widget.isWidgetVisible(ComponentID.BANK_PIN_CONTAINER);
    }

    /**
     * Banks items if your inventory does not have enough emptyslots (0 emptyslots being full). Will walk back to the initialplayerlocation passed as param
     *
     * @param itemNames
     * @param initialPlayerLocation
     * @param emptySlotCount
     * @return
     */
    public static boolean bankItemsAndWalkBackToOriginalPosition(List<String> itemNames, WorldPoint initialPlayerLocation, int emptySlotCount) {
        return bankItemsAndWalkBackToOriginalPosition(itemNames,false, getNearestBank(), initialPlayerLocation, emptySlotCount, 4);
    }

    /**
     * Banks items if your inventory is full. Will walk back to the initialplayerlocation passed as param
     *
     * @param itemNames
     * @param initialPlayerLocation
     * @return
     */
    public static boolean bankItemsAndWalkBackToOriginalPosition(List<String> itemNames, WorldPoint initialPlayerLocation) {
        return bankItemsAndWalkBackToOriginalPosition(itemNames,false, getNearestBank(), initialPlayerLocation, 0, 4);
    }

    /**
     * Banks at specific bank location if your inventory does not have enough emptyslots (0 emptyslots being full). Will walk back to the initialplayerlocation passed as param
     *
     * @param itemNames
     * @param exactItemNames
     * @param initialPlayerLocation
     * @param bankLocation
     * @param emptySlotCount
     * @param distance
     * @return
     */
    public static boolean bankItemsAndWalkBackToOriginalPosition(List<String> itemNames, boolean exactItemNames, BankLocation bankLocation, WorldPoint initialPlayerLocation, int emptySlotCount, int distance) {
        if (Rs2Inventory.emptySlotCount() <= emptySlotCount) {
            boolean isBankOpen = Rs2Bank.walkToBankAndUseBank(bankLocation);
            if (isBankOpen) {
                for (String itemName : itemNames) {
                    depositAll(itemName,false);
                    //Rs2Bank.depositAll(x -> x.name.toLowerCase().contains(itemName));
                }
            }
            return false;
        }

        if (distance > 10)
            distance = 10;

        if (initialPlayerLocation.distanceTo(Rs2Player.getWorldLocation()) > distance || !Rs2Tile.isTileReachable(initialPlayerLocation)) {
            Rs2Walker.walkTo(initialPlayerLocation, distance);
        } else {
            Rs2Walker.walkFastCanvas(initialPlayerLocation);
        }

        return !(Rs2Inventory.emptySlotCount() <= emptySlotCount) && initialPlayerLocation.distanceTo(Rs2Player.getWorldLocation()) <= distance;
    }

    /**
     * Banks items if your inventory does not have enough emptyslots (0 emptyslots being full). Will walk back to the initialplayerlocation passed as param
     *
     * @param itemNames
     * @param initialPlayerLocation
     * @param emptySlotCount
     * @param distance
     *
     * @return
     */
    public static boolean bankItemsAndWalkBackToOriginalPosition(List<String> itemNames, WorldPoint initialPlayerLocation, int emptySlotCount, int distance) {
        return bankItemsAndWalkBackToOriginalPosition(itemNames,false, getNearestBank(), initialPlayerLocation, emptySlotCount, distance);
    }

    public static boolean isWithdrawAs(boolean noted) {
        final boolean isNoted = Microbot.getVarbitValue(WITHDRAW_AS_NOTE_VARBIT) == 1;
        return isNoted == noted;
    }

    /**
     * Check if "noted" button is toggled on
     *
     * @return
     */
    public static boolean hasWithdrawAsNote() {
        return isWithdrawAs(true);
    }

    /**
     * Check if "item" button is toggled on
     *
     * @return
     */
    public static boolean hasWithdrawAsItem() {
        return isWithdrawAs(false);
    }

    private static final int NOTED_TOGGLE_WIDGET = 786451;

    public static boolean setWithdrawAs(boolean noted) {
        // If it’s already in the desired state, we’re done
        if (isWithdrawAs(noted)) return true;

        // Try up to 5 times to toggle and verify
        for (int i = 0; i < 5; i++) {
            Rs2Widget.clickWidget(NOTED_TOGGLE_WIDGET);
            if (sleepUntil(() -> isWithdrawAs(noted), 1200)) { // optional timeout
                return true;
            }
            Microbot.log("Retrying withdraw mode toggle attempt " + (i + 1));
        }

        // If all attempts failed
        return isWithdrawAs(noted);
    }


    /**
     * enable withdraw noted in your bank
     *
     * @return
     */
    public static boolean setWithdrawAsNote() {
        return setWithdrawAs(true);
    }

    /**
     * enable withdraw item in your bank
     *
     * @return
     */
    public static boolean setWithdrawAsItem() {
        return setWithdrawAs(false);
    }

    private static OptionalInt getRunePouch(IntPredicate filter) {
        return Arrays.stream(RunePouchType.values())
                .mapToInt(RunePouchType::getItemId)
                .filter(filter).findFirst();
    }

    /**
     * Withdraws the player's rune pouch if it's available in the bank.
     *
     * @return true if the rune pouch was withdrawn, false otherwise.
     */
    public static boolean withdrawRunePouch() {
        final OptionalInt pouch = getRunePouch(Rs2Bank::hasItem);
        return pouch.isPresent() && withdrawOne(pouch.getAsInt());
    }

    /**
     * Deposits the player's rune pouch if it's in the inventory.
     *
     * @return true if the rune pouch was deposited, false otherwise.
     */
    public static boolean depositRunePouch() {
        final OptionalInt pouch = getRunePouch(Rs2Inventory::hasItem);
        return pouch.isPresent() && depositOne(pouch.getAsInt());
    }

    /**
     * Checks if the player has any type of rune pouch in the bank.
     *
     * @return true if a rune pouch is found in the bank, false otherwise.
     */
    public static boolean hasRunePouch() {
        return Arrays.stream(RunePouchType.values())
                .anyMatch(pouch -> Rs2Bank.hasItem(pouch.getItemId()));
    }

    /**
     * Empty gem bag
     *
     * @return true if gem bag was emptied
     */

    public static boolean emptyGemBag() {
        Rs2ItemModel gemBag = Rs2Inventory.get(ItemID.GEM_BAG_12020,ItemID.OPEN_GEM_BAG);
        if (gemBag == null) return false;
        return Rs2Inventory.interact(gemBag, "Empty");
    }

    /**
     * Empty fish barrel
     *
     * @return true if fish barrel was emptied
     */

    public static boolean emptyFishBarrel() {
        Rs2ItemModel fishBarrel = Rs2Inventory.get(ItemID.FISH_BARREL,ItemID.OPEN_FISH_BARREL);
        if (fishBarrel == null) return false;
        return Rs2Inventory.interact(fishBarrel, "Empty");
    }

    /**
     * Empty herb sack
     *
     * @return true if herb sack was emptied
     */
    public static boolean emptyHerbSack() {
        Rs2ItemModel herbSack = Rs2Inventory.get(ItemID.HERB_SACK,ItemID.OPEN_HERB_SACK);
        if (herbSack == null) return false;
        return Rs2Inventory.interact(herbSack, "Empty");
    }

    /**
     * Empty seed box
     *
     * @return true if seed box was emptied
     */
    public static boolean emptySeedBox() {
        Rs2ItemModel seedBox = Rs2Inventory.get(ItemID.SEED_BOX,ItemID.OPEN_SEED_BOX);
        if (seedBox == null) return false;
        return Rs2Inventory.interact(seedBox, "Empty");
    }


    /**
     * Withdraw items from the lootTrackerPlugin
     *
     * @param npcName
     *
     * @return
     */
    public static boolean withdrawLootItems(String npcName, List<String> itemsToNotSell) {
        boolean isAtGe = Rs2GrandExchange.walkToGrandExchange();
        if (isAtGe) {
            boolean isBankOpen = Rs2Bank.useBank();
            if (!isBankOpen) return false;
        }
        Rs2Bank.depositAll();
        boolean itemFound = false;

        boolean hasWithdrawAsNote = Rs2Bank.setWithdrawAsNote();
        if (!hasWithdrawAsNote) return false;
        Rs2Bank.closeBank();
        return itemFound;
    }

    private static Widget getBankSizeWidget() {

        return Microbot.getClientThread().runOnClientThreadOptional(() -> {
            Widget bankContainerWidget = Microbot.getClient().getWidget(ComponentID.BANK_ITEM_COUNT_TOP);
            return bankContainerWidget;
        }).orElse(null);
    }

    /**
     * Retrieves the total number of items in the bank.
     * <p>
     * This method fetches the bank size widget and parses its text to determine
     * the total number of items currently stored in the bank.
     *
     * @return The total number of items in the bank. Returns 0 if the bank widget is not found.
     */
    public static int getBankItemCount() {
        Widget bank = getBankSizeWidget();
        if (bank == null) return 0;
        return Integer.parseInt(bank.getText());
    }

    /**
     * Retrieves an Rs2Item from the bank based on the specified item ID.
     *
     * @param itemId the ID of the item to search for.
     * @return the Rs2Item matching the item ID, or null if not found.
     */
    public static Rs2ItemModel getBankItem(int itemId) {
        return bankItems().stream()
                .filter(item -> item.getId() == itemId)
                .findFirst()
                .orElse(null);
    }

    /**
     * Retrieves an Rs2Item from the bank based on the specified item name.
     *
     * @param itemName the name of the item to search for.
     * @param exact whether to search for an exact match (true) or a partial match (false).
     * @return the Rs2Item matching the item name, or null if not found.
     */
    public static Rs2ItemModel getBankItem(String itemName, boolean exact) {
        return rs2BankData.getBankItems().stream()
                .filter(item -> exact
                        ? item.getName().equalsIgnoreCase(itemName)
                        : item.getName().toLowerCase().contains(itemName.toLowerCase()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Retrieves an Rs2Item from the bank based on a partial match of the specified item name.
     *
     * @param itemName the name of the item to search for.
     * @return the Rs2Item matching the item name (partial match), or null if not found.
     */
    public static Rs2ItemModel getBankItem(String itemName) {
        return getBankItem(itemName, false);
    }

    /**
     * Retrieves the list of bank tab widgets.
     * <p>
     * This method runs on the client thread to fetch the bank tab container widget
     * and then retrieves its dynamic children, which represent the tabs in the bank.
     * </p>
     *
     * @return A list of bank tab widgets, or null if the bank tab container widget is not found.
     */
    public static List<Widget> getTabs() {
        return Microbot.getClientThread().runOnClientThreadOptional(() -> {
            Widget bankContainerWidget = Microbot.getClient().getWidget(ComponentID.BANK_TAB_CONTAINER);
            if (bankContainerWidget != null) {
                // get children and filter out the tabs that don't have the Action Collapse tab
                return Arrays.asList(bankContainerWidget.getDynamicChildren());
            }
            return null;
        }).orElse(new ArrayList<>());
    }

    /**
     * Retrieves the list of item widgets in the bank container.
     * <p>
     * This method runs on the client thread to fetch the bank container widget
     * and then retrieves its dynamic children, which represent the items in the bank.
     * </p>
     *
     * @return A list of item widgets in the bank container, or null if the bank container widget is not found.
     */
    public static List<Widget> getItems() {
        return Microbot.getClientThread().runOnClientThreadOptional(() -> {
            Widget bankContainerWidget = Microbot.getClient().getWidget(BANK_ITEM_CONTAINER);
            if (bankContainerWidget != null) {
                // Get children and filter out the tabs that don't have the Action Collapse tab
                return Arrays.asList(bankContainerWidget.getDynamicChildren());
            }
            return null;
        }).orElse(Collections.emptyList());
    }

    /**
     * Retrieves the widget of an item based on the given slot ID.
     *
     * @param slotId the ID of the slot to retrieve the widget from
     *
     * @return the Widget associated with the specified slot ID, or null if the slot ID is out of range or if the items list is null
     */
    public static Widget getItemWidget(int slotId) {
        final List<Widget> items = getItems();
        if (slotId < 0 || slotId >= items.size()) return null;
        return items.get(slotId);
    }

    /**
     * Retrieves the bounding rectangle of an item widget based on the given slot ID.
     *
     * @param slotId the ID of the slot to retrieve the widget bounds from
     *
     * @return the bounds of the item widget as a Rectangle, or null if the widget is not found
     */
    public static Rectangle getItemBounds(int slotId) {
        Widget itemWidget = getItemWidget(slotId);
        if (itemWidget == null) return null;
        return itemWidget.getBounds();
    }

    /**
     * Gets the current tab index of the user's interface.
     *
     * @return the index of the currently selected tab
     */
    public static int getCurrentTab() {
        return Microbot.getVarbitValue(CURRENT_BANK_TAB);
    }

    /**
     * Checks if the main tab (index 0) is currently open.
     *
     * @return true if the main tab is open, false otherwise
     */
    public static boolean isMainTabOpen() {
        return isTabOpen(0);
    }

    /**
     * Checks if a tab with the given index is currently open.
     *
     * @param index the index of the tab to check
     *
     * @return true if the specified tab is open, false otherwise
     */
    public static boolean isTabOpen(int index) {
        return getCurrentTab() == index;
    }

    /**
     * Opens the main tab (index 0) in the user's interface.
     */
    public static void openMainTab() {
        openTab(0);
    }

    /**
     * Opens a tab based on the given index.
     *
     * @param index the index of the tab to open
     *              If the index is invalid or the tabs list is null, no action will be taken.
     */
    public static boolean openTab(int index) {
        final List<Widget> tabs = getTabs();
        if (index < 0 || index > tabs.size()) return false;
        Rs2Widget.clickWidgetFast(tabs.get(index + 10), 10 + index);
        Rs2Random.wait(100, 200);
        return true;
    }


    /**
     * Updates the item counts for each bank tab by retrieving values from corresponding variables.
     * This method fetches the item counts for each tab (1-9) and stores them in the bankTabCounts array.
     */
    private static void updateTabCounts() {
        bankTabCounts[0] = Microbot.getVarbitValue(BANK_TAB_ONE_COUNT);
        bankTabCounts[1] = Microbot.getVarbitValue(BANK_TAB_TWO_COUNT);
        bankTabCounts[2] = Microbot.getVarbitValue(BANK_TAB_THREE_COUNT);
        bankTabCounts[3] = Microbot.getVarbitValue(BANK_TAB_FOUR_COUNT);
        bankTabCounts[4] = Microbot.getVarbitValue(BANK_TAB_FIVE_COUNT);
        bankTabCounts[5] = Microbot.getVarbitValue(BANK_TAB_SIX_COUNT);
        bankTabCounts[6] = Microbot.getVarbitValue(BANK_TAB_SEVEN_COUNT);
        bankTabCounts[7] = Microbot.getVarbitValue(BANK_TAB_EIGHT_COUNT);
        bankTabCounts[8] = Microbot.getVarbitValue(BANK_TAB_NINE_COUNT);
    }

    /**
     * Determines the tab number that contains the item based on its slot ID.
     *
     * @param itemSlotId the slot ID of the item
     *
     * @return the 1-indexed tab number containing the item, or 0 if the item is in the main tab
     */
    private static int getItemTab(int itemSlotId) {
        int totalSlots = 0;

        // Loop through each tab's count and determine the tab for the item
        for (int i = 0; i < bankTabCounts.length; i++) {
            totalSlots += bankTabCounts[i];
            if (itemSlotId < totalSlots) {
                return i + 1; // Return tab number (1-indexed)
            }
        }

        // If itemSlotId is above all the tabs, it is in tab 0
        return 0;
    }

    /**
     * Retrieves the tab number of a bank item based on its slot ID.
     * Updates the tab counts before determining which tab the item belongs to.
     *
     * @param itemSlotId the slot ID of the bank item
     *
     * @return the tab number containing the item, or -1 if the slot ID is invalid
     */
    public static int getItemTabForBankItem(int itemSlotId) {
        // Update tab counts before checking which tab the item is in
        updateTabCounts();

        // Get the total number of items in the bank
        int totalItemsInBank = getBankItemCount();

        // Ensure the slot ID is within valid range
        if (itemSlotId < 0 || itemSlotId >= totalItemsInBank) {
            return -1;  // Invalid slot ID
        }

        // Determine which tab the item is in
        return getItemTab(itemSlotId);
    }

    /**
     * Counts the partial rows present in each tab based on the number of items in each tab.
     * A partial row is considered if a tab does not have enough items to fully fill a row.
     *
     * @return an array of integers where each element is 1 if the tab has a partial row, 0 otherwise
     */
    private static int[] countPartialRowsInTabs() {
        int[] partialRowCounts = new int[bankTabCounts.length];

        for (int i = 0; i < bankTabCounts.length; i++) {
            int totalItemsInTab = bankTabCounts[i];

            // If there's a remainder, then there is a partially filled row
            if (totalItemsInTab % BANK_ITEMS_PER_ROW != 0) {
                partialRowCounts[i] = 1;
            } else {
                partialRowCounts[i] = 0;
            }
        }

        return partialRowCounts;
    }

    /**
     * Calculates the total number of partial rows in the bank across all specified tabs.
     *
     * @param numberOfTabs the number of tabs to consider when calculating partial rows
     *
     * @return the total number of partial rows in the specified tabs
     */
    private static int calculatePartialRowsInBank(int numberOfTabs) {
        int totalPartialRows = 0;

        // Get the partial row counts for each tab
        int[] partialRowCounts = countPartialRowsInTabs();

        // Calculate the total number of partial rows
        for (int i = 0; i < numberOfTabs; i++) {
            totalPartialRows += partialRowCounts[i];
        }

        return totalPartialRows;
    }

    /**
     * Calculates the vertical scroll position (scrollY) required to make a specific item visible in the bank view.
     *
     * @param slotId the slot ID of the item to scroll to
     *
     * @return the calculated scrollY value needed to display the item at the top of the bank view
     */
    private static int calculateScrollYFromSlotId(int slotId) {
        int row;
        int scrollY;
        // Get total items in tabs 1-9
        int totalItemsInTabs1To9 = bankTabCounts[0] + bankTabCounts[1] + bankTabCounts[2] + bankTabCounts[3] +
                bankTabCounts[4] + bankTabCounts[5] + bankTabCounts[6] + bankTabCounts[7] +
                bankTabCounts[8];

        // Get the current tab selected
        int currentTab = getCurrentTab();

        // Calculate the rows only within the selected tab
        if (currentTab == 0) {
            // Determine if the slotId belongs to tab 0 or one of the other tabs
            if (slotId >= totalItemsInTabs1To9) {
                // The item belongs to tab 0
                int tab0SlotId = slotId - totalItemsInTabs1To9;
                row = tab0SlotId / BANK_ITEMS_PER_ROW;
            } else {
                // The item belongs to tabs 1-9
                int totalItemsInBank = getBankItemCount();
                int tab0SlotId = slotId + (totalItemsInBank - totalItemsInTabs1To9);

                row = tab0SlotId / BANK_ITEMS_PER_ROW;
                row += calculatePartialRowsInBank(getItemTab(slotId));
            }
        } else {
            // If a tab from 1-9 is selected, calculate rows within that tab
            int itemsBeforeSelectedTab = 0;
            for (int i = 0; i < currentTab - 1; i++) {
                itemsBeforeSelectedTab += bankTabCounts[i];  // Sum items from previous tabs
            }

            // Calculate the position relative to the selected tab
            int tabSlotId = slotId - itemsBeforeSelectedTab;

            // Only calculate rows within the selected tab
            row = tabSlotId / BANK_ITEMS_PER_ROW;
        }

        // Calculate the scrollY based on the row within the selected tab
        scrollY = row * (BANK_ITEM_HEIGHT + BANK_ITEM_Y_PADDING);

        // Get the widget that displays the bank items
        Widget w = Microbot.getClient().getWidget(BANK_ITEM_CONTAINER);

        // Check the height of the bank window to adjust scrolling if necessary
        assert w != null;
        int bankHeight = w.getHeight() / (BANK_ITEM_HEIGHT + BANK_ITEM_Y_PADDING);

        // Calculate the minimum scrollY to ensure the item is visible at the top of the window
        // This would be the scrollY that places the item's row at the very top of the visible area
        int minScrollY = scrollY - (bankHeight) * (BANK_ITEM_HEIGHT + BANK_ITEM_Y_PADDING);

        // Ensure that minScrollY is non-negative, since scrollY cannot be negative
        if (minScrollY < 0) {
            minScrollY = 0;
        }

        // check if the item is already visible by checking if currentScrollY is a value between minScrollY and scrollY
        int currentScrollY = w.getScrollY();
        if (currentScrollY >= minScrollY && currentScrollY <= scrollY) {
            return currentScrollY;
        }

        if (minScrollY == 0)
            return minScrollY;

        // return a value that is within the bounds of the scroll bar
        return Rs2Random.nextInt(minScrollY, scrollY, 0.5, true);
    }

    /**
     * Scrolls the bank view to make a specified item slot visible.
     *
     * @param slotId the slot ID of the item to scroll to
     */
    public static boolean scrollBankToSlot(int slotId) {
        int scrollY = calculateScrollYFromSlotId(slotId);
        Widget w = Microbot.getClient().getWidget(BANK_ITEM_CONTAINER);
        if (w == null) return false;

        Microbot.getClientThread().invokeLater(() -> {
            Microbot.getClient().setVarcIntValue(VarClientInt.BANK_SCROLL, scrollY);
            Microbot.getClient().runScript(ScriptID.UPDATE_SCROLLBAR, ComponentID.BANK_SCROLLBAR, BANK_ITEM_CONTAINER, scrollY);
        });
        w.setScrollY(scrollY);
        return true;
    }


    /**
     * Tries to hover the mouse over the bank object or bank NPC.
     *
     * @return True if bank was successfully hovered over, otherwise false.
     */
    public static boolean preHover() {
        if (!Rs2AntibanSettings.naturalMouse) {
            if(Rs2AntibanSettings.devDebug)
                Microbot.log("Natural mouse is not enabled, can't hover");
            return false;
        }

        if (isOpen()) {
            return false;
        }

        Microbot.status = "Hovering over bank";

        try {
            GameObject bank = Rs2GameObject.findBank();
            if (bank != null) {
                return hoverOverObject(bank);
            }

            WallObject grandExchangeBooth = Rs2GameObject.findGrandExchangeBooth();
            if (grandExchangeBooth != null) {
                return hoverOverObject(grandExchangeBooth);
            }

            Rs2NpcModel npc = Rs2Npc.getBankerNPC();
            if (npc != null) {
                return hoverOverActor(npc);
            }

            Microbot.log("No bank objects or NPC found to hover over.");
        } catch (Exception ex) {
            Microbot.log("An error occurred while hovering over the bank: " + ex.getMessage());
        }

        return false;
    }


    private static boolean hasKeyboardBankPinEnabled() {
        return Microbot.getConfigManager().getConfiguration("bank","bankPinKeyboard").equalsIgnoreCase("true");

    }

    /**
     * Checks whether the given widget represents a locked bank slot.
     *
     * This inspects the widget’s context actions for the “Unlock-slot” option,
     * which indicates the slot is currently locked. If the widget is null or has
     * no actions, it is considered not locked.
     *
     * @param widget the bank-slot widget to inspect; may be null
     * @return true if the widget’s actions contain “Unlock-slot” (case-insensitive), false otherwise
     */
    private static boolean isWidgetLocked(Widget widget) {
        if (widget == null) return false;
        String[] actions = widget.getActions();
        return actions != null && Arrays.stream(actions).filter(Objects::nonNull).anyMatch("Unlock-slot"::equalsIgnoreCase);
    }

    /**
     * Determines whether the bank inventory slot at the given index is currently locked.
     *
     * This method checks that the slot index is within the valid 0–27 range, obtains the bank
     * inventory widget container, verifies the widget children array is present and that the
     * specified index exists, then delegates to isWidgetLocked(...) on the child widget.
     *
     * Preconditions:
     * - The slot index must be >= 0 and < 28.
     * - The bank inventory widget (BANK_INVENTORY_ITEM_CONTAINER) must be loaded in the client.
     *
     * @param slot the inventory slot index to check (0-based)
     * @return true if the widget for this slot is non-null and its actions include “Unlock-slot”,
     *         indicating the slot is currently locked; false if out of range, widget missing, or not locked
     */
    public static boolean isLockedSlot(int slot) {
        if (slot < 0 || slot >= 28) return false;
        Widget container = Microbot.getClient().getWidget(BANK_INVENTORY_ITEM_CONTAINER);
        if (container == null || container.getChildren() == null || slot >= container.getChildren().length)
            return false;
        return isWidgetLocked(container.getChild(slot));
    }

    /**
     * Scans the bank inventory widget for slots currently locked and returns their indices.
     *
     * Iterates through each child widget of the bank inventory container, logs debug info
     * about null widgets or their actions, and collects indices where isWidgetLocked(...)
     * returns true. If the container or its children are unavailable, returns an empty list.
     *
     * Preconditions:
     * - The client must have the bank inventory UI loaded so that BANK_INVENTORY_ITEM_CONTAINER
     *   is present with a non-null children array.
     *
     * Postconditions:
     * - Returns a list of slot indices (0-based) where the widget’s actions include “Unlock-slot”.
     * - If no locked slots are found or UI is unavailable, returns an empty list.
     *
     * Side Effects:
     * - Emits debug logs for container presence, each slot’s actions, and whether locked slots were detected.
     *
     * @return List of indices of locked slots; empty if none or if the bank UI isn’t ready.
     */
    public static List<Integer> findLockedSlots() {
        List<Integer> lockedSlots = new ArrayList<>();
        Widget container = Microbot.getClient().getWidget(BANK_INVENTORY_ITEM_CONTAINER);
        if (container == null || container.getChildren() == null) {
            log.debug("Bank inventory container is null or has no children.");
            return lockedSlots;
        }
        Widget[] items = container.getChildren();
        for (int i = 0; i < items.length; i++) {
            Widget w = items[i];
            if (w == null) {
                log.debug("Slot {}: null widget", i);
                continue;
            }
            log.debug("Slot {}: actions = {}", i, Arrays.toString(w.getActions()));
            if (isWidgetLocked(w)) {
                log.debug("Found locked slot at {}", i);
                lockedSlots.add(i);
            }
        }
        if (lockedSlots.isEmpty()) {
            log.debug("No locked slots detected.");
        }
        return lockedSlots;
    }

    /**
     * Toggles the lock state of the given bank inventory item.
     *
     * Checks preconditions: the item must be non-null, the bank must be open,
     * the inventory must contain the item, and the bank’s lock UI options must be enabled.
     * It reads the current OVERVIEW varbit before invoking the “Lock/Unlock slot” menu action,
     * then waits until that varbit changes, indicating the lock state flipped.
     *
     * Preconditions:
     * - rs2Item is not null.
     * - Bank interface is open (isOpen() == true).
     * - Inventory contains the item ID (Rs2Inventory.hasItem(...)).
     * - BANK_SIDE_SLOT_IGNOREINVLOCKS varbit == 0 (lock feature enabled).
     * - BANK_SIDE_SLOT_SHOWOP varbit == 1 (locking option visible).
     *
     * Postconditions:
     * - invokeMenu(10, rs2Item) is called to trigger the lock/unlock action.
     * - Returns true if the OVERVIEW varbit changes within the timeout, indicating a successful toggle.
     * - Returns false immediately if any precondition fails or if the varbit does not change within the timeout.
     *
     * @param rs2Item the inventory item model to lock or unlock
     * @return true if the lock state changed (detected via varbit change); false otherwise
     */
    private static boolean toggleItemLock(Rs2ItemModel rs2Item)
    {
        if (rs2Item == null
                || !isOpen()
                || !Rs2Inventory.hasItem(rs2Item.getId())
                || Microbot.getVarbitValue(VarbitID.BANK_SIDE_SLOT_IGNOREINVLOCKS) != 0
                || Microbot.getVarbitValue(VarbitID.BANK_SIDE_SLOT_SHOWOP) != 1) {
            return false;
        }
        container = BANK_INVENTORY_ITEM_CONTAINER;
        final int currentLockState = Microbot.getVarbitValue(VarbitID.BANK_SIDE_SLOT_OVERVIEW);
        invokeMenu(10, rs2Item);
        return sleepUntilTrue(() -> Microbot.getVarbitValue(VarbitID.BANK_SIDE_SLOT_OVERVIEW) != currentLockState, 300, 2000);
    }

    /**
     * Toggles the lock state of an inventory item by its name.
     *
     * Looks up the first inventory item whose name matches (exactly or partially),
     * then delegates to toggleItemLock(Rs2ItemModel) to perform the lock/unlock action.
     *
     * Preconditions:
     * - Bank interface must be open and the item must exist in inventory.
     * - Varbit checks (ignore-locks and show-option) are handled in the delegated method.
     *
     * @param itemName the name of the item to toggle lock on
     * @param exact if true, matches name exactly (case-insensitive); if false, matches if name contains the given string
     * @return true if the lock state was toggled successfully; false if item not found or toggle conditions not met
     */
    public static boolean toggleItemLock(String itemName, boolean exact)
    {
        Rs2ItemModel item = Rs2Inventory.get(itemName, exact);
        return toggleItemLock(item);
    }

    /**
     * Toggles the lock state of all currently locked bank inventory slots.
     *
     * Scans for slots flagged as locked via findLockedSlots(); if none are found, logs and returns false.
     * Otherwise, for each locked slot, obtains the item in that slot and invokes toggleItemLock(item),
     * logging success or failure. Returns true if at least one slot was toggled.
     *
     * Preconditions:
     * - The bank UI must be open and loaded so that findLockedSlots() can detect locked slots.
     * - toggleItemLock(...) handles its own checks (bank open, varbits).
     *
     * Postconditions:
     * - Each slot returned by findLockedSlots() has had toggleItemLock called on its item model.
     * - Returns true if there were locked slots and at least one toggleItemLock(...) returned true;
     *   returns false if no locked slots were found or none were successfully toggled.
     *
     * Side Effects:
     * - Logs debug messages for absence of locked slots, missing items, successes, and failures.
     *
     * @return true if one or more locked slots were toggled; false if no locked slots existed or none could be toggled
     */
    public static boolean toggleAllLocks() {
        List<Integer> lockedSlots = findLockedSlots();
        if (lockedSlots.isEmpty()) {
            log.debug("No locked slots to toggle.");
            return false;
        }
        boolean anyUnlocked = !findLockedSlots().isEmpty();
        for (int slot : lockedSlots) {
            Rs2ItemModel item = Rs2Inventory.getItemInSlot(slot);
            if (item == null) {
                log.debug("Slot {}: No item found, skipping.", slot);
                continue;
            }
            if (toggleItemLock(item)) {
                log.debug("Unlocked item in slot {}", slot);
                anyUnlocked = true;
            } else {
                log.debug("Failed to unlock item in slot {}", slot);
            }
        }
        return anyUnlocked;
    }

    /**
     * Locks items in the specified inventory slot indices.
     *
     * Iterates through each provided slot index, validates the index range (0–27),
     * retrieves the item in that slot, skips if empty or already locked, and invokes
     * toggleItemLock(...) to lock it. Returns true if at least one item was successfully locked.
     *
     * Preconditions:
     * - The bank interface must be open and the inventory widgets loaded so that
     *   Rs2Inventory.getItemInSlot(slot) and isLockedSlot(slot) work correctly.
     * - The slots array should correspond to actual inventory slots.
     *
     * Postconditions:
     * - For each valid slot with an item not already locked, toggleItemLock is called.
     * - Logs debug messages for invalid indices, empty slots, already locked items,
     *   successful locks, or failures.
     * - Returns true if any toggleItemLock(...) returned true; false if none were locked
     *   or if no valid slots were provided.
     *
     * @param slots varargs of inventory slot indices to lock
     * @return true if one or more items were locked; false otherwise
     */
    public static boolean lockAllBySlot(int... slots) {
        if (slots == null || slots.length == 0) {
            log.debug("No slot indices provided for locking.");
            return false;
        }
        boolean anyLocked = false;
        for (int slot : slots) {
            if (slot < 0 || slot >= 28) {
                log.debug("Invalid slot index: {}", slot);
                continue;
            }
            Rs2ItemModel item = Rs2Inventory.getItemInSlot(slot);
            if (item == null) {
                log.debug("No item found in slot {}", slot);
                continue;
            }
            if (isLockedSlot(slot)) {
                log.debug("Item '{}' in slot {} is already locked.", item.getName(), slot);
                continue;
            }
            if (toggleItemLock(item)) {
                log.debug("Locked item '{}' in slot {}", item.getName(), slot);
                anyLocked = true;
            } else {
                log.debug("Failed to lock item '{}' in slot {}", item.getName(), slot);
            }
        }
        return anyLocked;
    }
}