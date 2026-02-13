package net.runelite.client.plugins.microbot.MossKiller;

import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemID;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.breakhandler.BreakHandlerPlugin;
import net.runelite.client.plugins.microbot.breakhandler.BreakHandlerScript;
import net.runelite.client.plugins.microbot.shortestpath.ShortestPathPlugin;
import net.runelite.client.plugins.microbot.playermonitor.PlayerMonitorPlugin;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.antiban.enums.ActivityIntensity;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.bank.enums.BankLocation;
import net.runelite.client.plugins.microbot.util.camera.Rs2Camera;
import net.runelite.client.plugins.microbot.util.combat.Rs2Combat;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange;
import net.runelite.client.plugins.microbot.util.grounditem.Rs2GroundItem;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.magic.Rs2CombatSpells;
import net.runelite.client.plugins.microbot.util.magic.Rs2Magic;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;
import net.runelite.client.plugins.microbot.util.models.RS2Item;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.security.Login;
import net.runelite.client.plugins.microbot.util.tabs.Rs2Tab;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import net.runelite.client.plugins.skillcalculator.skills.MagicAction;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.runelite.api.EquipmentInventorySlot.AMMO;
import static net.runelite.api.ItemID.*;
import static net.runelite.api.NpcID.MOSS_GIANT_2093;
import static net.runelite.api.Skill.*;
import static net.runelite.api.gameval.ItemID.*;
import static net.runelite.api.gameval.ItemID.AMULET_OF_MAGIC;
import static net.runelite.api.gameval.ItemID.AMULET_OF_POWER;
import static net.runelite.api.gameval.ItemID.APPLE_PIE;
import static net.runelite.api.gameval.ItemID.BIG_BONES;
import static net.runelite.api.gameval.ItemID.BLACK_2H_SWORD;
import static net.runelite.api.gameval.ItemID.BLACK_SQ_SHIELD;
import static net.runelite.api.gameval.ItemID.BONES;
import static net.runelite.api.gameval.ItemID.BREAD;
import static net.runelite.api.gameval.ItemID.BRONZE_2H_SWORD;
import static net.runelite.api.gameval.ItemID.BRONZE_ARROW;
import static net.runelite.api.gameval.ItemID.BRONZE_AXE;
import static net.runelite.api.gameval.ItemID.BRONZE_PICKAXE;
import static net.runelite.api.gameval.ItemID.BURNT_MEAT;
import static net.runelite.api.gameval.ItemID.COINS;
import static net.runelite.api.gameval.ItemID.COOKED_MEAT;
import static net.runelite.api.gameval.ItemID.FISHING_BAIT;
import static net.runelite.api.gameval.ItemID.FISHING_ROD;
import static net.runelite.api.gameval.ItemID.HALF_AN_APPLE_PIE;
import static net.runelite.api.gameval.ItemID.HAMMER;
import static net.runelite.api.gameval.ItemID.IRON_2H_SWORD;
import static net.runelite.api.gameval.ItemID.IRON_ARROW;
import static net.runelite.api.gameval.ItemID.KNIFE;
import static net.runelite.api.gameval.ItemID.LEATHER_BOOTS;
import static net.runelite.api.gameval.ItemID.LEATHER_VAMBRACES;
import static net.runelite.api.gameval.ItemID.LOGS;
import static net.runelite.api.gameval.ItemID.MAPLE_LONGBOW;
import static net.runelite.api.gameval.ItemID.MAPLE_SHORTBOW;
import static net.runelite.api.gameval.ItemID.MITHRIL_2H_SWORD;
import static net.runelite.api.gameval.ItemID.MITHRIL_ARROW;
import static net.runelite.api.gameval.ItemID.MITHRIL_AXE;
import static net.runelite.api.gameval.ItemID.MITHRIL_SWORD;
import static net.runelite.api.gameval.ItemID.MOSSY_KEY;
import static net.runelite.api.gameval.ItemID.OAK_LOGS;
import static net.runelite.api.gameval.ItemID.OAK_SHORTBOW;
import static net.runelite.api.gameval.ItemID.RUNE_CHAINBODY;
import static net.runelite.api.gameval.ItemID.RUNE_SCIMITAR;
import static net.runelite.api.gameval.ItemID.SHORTBOW;
import static net.runelite.api.gameval.ItemID.SPADE;
import static net.runelite.api.gameval.ItemID.STAFF_OF_AIR;
import static net.runelite.api.gameval.ItemID.STAFF_OF_FIRE;
import static net.runelite.api.gameval.ItemID.STAFF_OF_WATER;
import static net.runelite.api.gameval.ItemID.STEEL_2H_SWORD;
import static net.runelite.api.gameval.ItemID.STEEL_ARROW;
import static net.runelite.api.gameval.ItemID.STEEL_BAR;
import static net.runelite.api.gameval.ItemID.STEEL_KITESHIELD;
import static net.runelite.api.gameval.ItemID.STUDDED_BODY;
import static net.runelite.api.gameval.ItemID.STUDDED_CHAPS;
import static net.runelite.api.gameval.ItemID.SWORDFISH;
import static net.runelite.api.gameval.ItemID.TINDERBOX;
import static net.runelite.api.gameval.ItemID.UNCUT_DIAMOND;
import static net.runelite.api.gameval.ItemID.UNCUT_RUBY;
import static net.runelite.api.gameval.ItemID.WILLOW_SHORTBOW;
import static net.runelite.client.plugins.microbot.globval.GlobalWidgetInfo.COMBAT_STYLE_FOUR;
import static net.runelite.client.plugins.microbot.MossKiller.Enums.AttackStyle.MAGIC;
import static net.runelite.client.plugins.microbot.MossKiller.Enums.AttackStyle.RANGE;
import static net.runelite.client.plugins.microbot.MossKiller.MossKillerScript.CHAOS_RUNE;
import static net.runelite.client.plugins.microbot.MossKiller.MossKillerScript.LAW_RUNE;
import static net.runelite.client.plugins.microbot.util.bank.Rs2Bank.*;
import static net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange.sellLoot;
import static net.runelite.client.plugins.microbot.util.npc.Rs2Npc.getNpcs;
import static net.runelite.client.plugins.microbot.util.player.Rs2Player.getHealthPercentage;
import static net.runelite.client.plugins.microbot.util.walker.Rs2Walker.walkFastCanvas;
import static net.runelite.client.plugins.microbot.util.walker.Rs2Walker.walkTo;
import static net.runelite.client.plugins.skillcalculator.skills.MagicAction.HIGH_LEVEL_ALCHEMY;

public class WildySaferScript extends Script {

    @Inject
    MossKillerPlugin mossKillerPlugin;

    @Inject
    WildyKillerScript wildyKillerScript;

    @Inject
    MossKillerScript mossKillerScript;

    @Inject
    Client client;

    @Inject
    private MossKillerConfig mossKillerConfig;

    private static MossKillerConfig config;


    @Inject
    public WildySaferScript(MossKillerConfig config) {
        WildySaferScript.config = config;
    }

    private static int[] LOOT_LIST = new int[]{MOSSY_KEY, LAW_RUNE, AIR_RUNE, EARTH_RUNE, IRON_ARROW, COSMIC_RUNE, CHAOS_RUNE, DEATH_RUNE, NATURE_RUNE, UNCUT_RUBY, UNCUT_DIAMOND, STEEL_BAR, STEEL_ARROW};
    private static int[] ALCHABLES = new int[]{STEEL_KITESHIELD, MITHRIL_SWORD, BLACK_SQ_SHIELD};
    public final WorldPoint monkPoint = new WorldPoint(3051, 3490, 0);
    private static final WorldArea SAFE_ZONE_AREA = new WorldArea(3130, 3822, 30, 20, 0);
    public static final WorldPoint SAFESPOT = new WorldPoint(3137, 3833, 0);
    public static final WorldPoint SAFESPOT1 = new WorldPoint(3137, 3831, 0);
    private static boolean missingConsumablesOnce = false;
    private static boolean missingEquipmentOnce = false;
    private volatile boolean isWalking = false;
    private boolean noArrows = false;
    private boolean mithArrows = false;
    private boolean bronzeArrows = false;
    public int[] LOOT_LIST2 = new int[]{COSMIC_RUNE,DEATH_RUNE,STEEL_BAR,UNCUT_DIAMOND,UNCUT_RUBY,STEEL_ARROW};
    private int[] alchables = null; // Initialize as null
    private int banana = 0;

    // Lazy initialization method
    private int[] getAlchables() {
        if (alchables == null) {
            alchables = new int[]{
                    ItemID.STEEL_KITESHIELD,
                    ItemID.MITHRIL_SWORD,
                    ItemID.BLACK_SQ_SHIELD
            };
        }
        return alchables;
    }


    private boolean flag1 = false;
    public boolean fired = false;
    public boolean move = false;
    public boolean safeSpot1Attack = false;
    public boolean iveMoved = false;
    private boolean freshieRound = false;
    public int playerCounter = 0;
    private boolean moreThan100k = false;

    private boolean setItOnce = false;
    public static boolean test = false;
    public boolean run(MossKillerConfig config) {
        if (mainScheduledFuture != null && !mainScheduledFuture.isCancelled() && !mainScheduledFuture.isDone()) {
            Microbot.log("Scheduled task already running.");
            return false;
        }
        Microbot.enableAutoRunOn = false;
        Rs2AntibanSettings.naturalMouse = true;
        Rs2AntibanSettings.simulateMistakes = true;
        Rs2Antiban.setActivityIntensity(ActivityIntensity.MODERATE);
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {

                if (!Microbot.isLoggedIn()) {
                    Microbot.log("Not logged in, skipping tick.");
                    return;}
                if (!super.run()) {Microbot.log("super.run() returned false, skipping tick. moss");
                    return;}
                if (Microbot.getClient() == null || Microbot.getClient().getLocalPlayer() == null) {
                    Microbot.log("Client or local player not ready. Skipping tick.");
                    return;
                }
                if (!Microbot.isPluginEnabled(BreakHandlerPlugin.class)) {Microbot.log("No breakhandler moss safer"); return;}
                /*Microbot.AccountSessionData data = Microbot.accountSessionMap.get(Microbot.activeAccountIndex);
                if (data != null && data.isFreshie && data.freshieRoundCount <= 20 && !freshieRound) {
                    BreakHandlerScript.breakIn = 20 * 60;
                    Microbot.log("Freshie detected and it's round is: " + data.freshieRoundCount + " reducing breaktime");
                    data.freshieRoundCount++;
                    Microbot.accountSessionMap.put(Microbot.activeAccountIndex, data);
                    freshieRound = true;
                    return;}*/
                if (Microbot.bonesToBananas) return;
                if (Microbot.herringTime) return;
                if (!Microbot.safeMossOn && Microbot.lock) return;

                long startTime = System.currentTimeMillis();

                if (mossKillerPlugin.preparingForShutdown) {
                    MossKillerScript.prepareSoftStop();}

                if (Rs2Inventory.contains(MOSSY_KEY)) {
                    doBankingLogic();
                }

                if (!setItOnce && Rs2Player.getWorldLocation().getY() > 3820 && Rs2Magic.isNotFireStrikeAutocast() ) {
                    Rs2Combat.setAutoCastSpell(Rs2CombatSpells.FIRE_STRIKE, true);
                    Microbot.log("60 second sleep now, goodluck, don't die");
                    sleep(60000);
                    setItOnce = true;
                }

                if (mossKillerPlugin.isSuperJammed()) {
                    if (Rs2GameObject.exists(39549)){
                        sleep(15000);
                        Rs2GameObject.interact(39549, "Use");
                        sleep(15000);}
                    /// updates autologin script to accomodate stew script
                    //AutoLoginScript.stopMonkKillerPlugin();
                    Microbot.monkKillerOn = true;
                }

                if (BreakHandlerScript.breakIn <= 420) {
                Microbot.status = "preparing for monks";
                    sleepUntil(() -> !Rs2Player.isMoving());
                    doMonkBankingLogic();
                    if (!Microbot.bonesToBananas && !Microbot.herringTime) walkTo(monkPoint);
                    if (Rs2Inventory.contains(BIG_BONES) && (!Microbot.bonesToBananas && !Microbot.herringTime)) {doMonkBankingLogic();}
                    if (!Microbot.bonesToBananas && !Microbot.herringTime) walkTo(monkPoint);
                    if (!Microbot.bonesToBananas && !Microbot.herringTime) {BreakHandlerScript.breakIn = 15;
                        // 5-10 minutes of potential sleep, to force a disconnect logout if breakhandler or Rs2Player.logout() doesn't succeed in logging out
                    if (!sleepUntil(() -> Microbot.getClient().getGameState() == GameState.LOGIN_SCREEN, 300000))
                        Rs2Player.logout(); if (!sleepUntil(() -> Microbot.getClient().getGameState() == GameState.LOGIN_SCREEN, 310000))
                            Rs2Player.logout();} else {BreakHandlerScript.breakIn = 2000; Microbot.log("Extending break for bonesToBananas or herringTime");}
                    freshieRound = false;
                    if (Microbot.bonesToBananas || Microbot.herringTime) {stopMossKillerPlugin();}
                    return;
                }

                if (mossKillerPlugin.shouldHopWorld()) {
                    Rs2Bank.closeBank();
                    sleep(1200);
                    Microbot.log("Player is dead! Hopping worlds...");
                    sleepUntil(() -> !Rs2Player.isInCombat());
                    sleep(4900);
                    Rs2Player.logout();
                    sleep(2900);
                    mossKillerPlugin.resetWorldHopFlag(); // Reset after hopping
                }

                //if you're at moss giants and your inventory is not prepared, prepare inventory
                if (isInMossGiantArea() && !isInventoryPreparedMage()) {
                    if (config.attackStyle() == MAGIC) {doBankingLogic();}
                    if (config.attackStyle() == RANGE) {if (!isInventoryPreparedArcher()) {doBankingLogic();}}
                    if (config.attackStyle() == RANGE) {if (!equipmentIsPrepared()) {doBankingLogic();}}
                }

                //if you're not at moss giants and your inventory is not prepared, prepare inventory
                if (!isInMossGiantArea() && !isInventoryPreparedMage()) {
                    if (config.attackStyle() == MAGIC) {doBankingLogic();}
                    if (config.attackStyle() == RANGE) {if (!isInventoryPreparedArcher()) {doBankingLogic();}}
                }
                // If you're not at moss giants but have prepared inventory, go to moss giants
                if (!isInMossGiantArea() && equipmentIsPrepared()) {
                    System.out.println("not in moss giant area but we are prepared");
                    if (config.attackStyle() == MAGIC && isWearingWithRequiredItems() && isInventoryPreparedMage()) {walkTo(SAFESPOT);} else if (config.attackStyle() == MAGIC){doBankingLogic();}
                    if (config.attackStyle() == RANGE && isWearingWithRequiredItemsRange() && isInventoryPreparedArcher()) {walkTo(SAFESPOT);} else if (config.attackStyle() == RANGE){doBankingLogic();
                        return;}
                    // if you're not at moss giants but don't have prepared inventory, prepare inventory
                }

                if (!isInMossGiantArea() && isInventoryPreparedMage() && !equipmentIsPrepared()) {
                    doBankingLogic();
                    return;
                }

                if (!isInMossGiantArea() && isInventoryPreparedArcher() && !equipmentIsPrepared()) {
                    doBankingLogic();
                    return;
                }

                if (!isInMossGiantArea() && !isInventoryPreparedArcher() && !equipmentIsPrepared()) {
                    doBankingLogic();
                    return;
                }

                if (isInMossGiantArea() && !Rs2Equipment.isWearing(AMMO)) {
                    if(!Rs2Inventory.interact(MITHRIL_ARROW, "wield")) {Rs2Inventory.interact(BRONZE_ARROW, "wield"); }
                }

                WorldPoint spot9 = new WorldPoint (3137, 3832,0);
                WorldPoint spot8 = new WorldPoint (3138, 3833,0);
                if (isInMossGiantArea() &&
                        (Rs2Player.getLocalPlayer().getWorldLocation().equals(spot8) || Rs2Player.getLocalPlayer().getWorldLocation().equals(spot9))) {
                    Microbot.log("testing new unstucker");
                    walkFastCanvas(SAFESPOT);
                }

                    // If at safe area of moss giants and there is items to loot, loot them
                if (isInMossGiantArea() && itemsToLoot() && (!isAnyMossGiantInteractingWithMe() || (Rs2Player.getLocalPlayer().getWorldLocation().equals(spot8) || Rs2Player.getLocalPlayer().getWorldLocation().equals(spot9)))) {
                    lootItems();
                    if (config.attackStyle() == RANGE && (Rs2Inventory.contains(MITHRIL_ARROW) || Rs2Inventory.contains(BRONZE_ARROW))) {
                        System.out.println("getting here?");
                        if(!Rs2Inventory.interact(MITHRIL_ARROW, "wield")) {Rs2Inventory.interact(BRONZE_ARROW, "wield"); }
                    }
                }
                // If not at the safe spot but in the safe zone area, go to the safe spot
                if (!isAtSafeSpot() && !iveMoved && isInMossGiantArea()) {
                    System.out.println("not at safe spot but in moss giant area");
                    walkFastCanvas(SAFESPOT);
                    sleep(1200,2000);
                    return;
                }

                //if using magic make sure autocast is on
                if (config.attackStyle() == MAGIC && Rs2Equipment.isWearing(STAFF_OF_FIRE)
                        && !mossKillerPlugin.getAttackStyle()) {
                    if (!config.forceDefensive()){
                        Rs2Combat.setAutoCastSpell(Rs2CombatSpells.FIRE_STRIKE, false);}
                    else Rs2Combat.setAutoCastSpell(Rs2CombatSpells.FIRE_STRIKE, true);
                }

                //if using magic make sure staff is equipped
                if (config.attackStyle() == MAGIC && !Rs2Equipment.isWearing(STAFF_OF_FIRE) && Rs2Inventory.contains(STAFF_OF_FIRE)) {
                    Rs2Inventory.equip(STAFF_OF_FIRE);
                }
                //if using magic make sure staff you have a staff in your possesion
                if (config.attackStyle() == MAGIC && !Rs2Equipment.isWearing(STAFF_OF_FIRE) && !Rs2Inventory.contains(STAFF_OF_FIRE)) {
                    doBankingLogic();
                }

                Rs2Player.eatAt(70);

                if (Rs2Inventory.contains(NATURE_RUNE) &&
                        !Rs2Inventory.contains(STAFF_OF_FIRE) &&
                        Rs2Inventory.contains(ALCHABLES) &&
                        config.alchLoot()) {

                    boolean canAlch = true;

                    // Rangers need fire runes for alching
                    if (config.attackStyle() == RANGE && !Rs2Inventory.contains(FIRE_RUNE, 5)) {
                        canAlch = false; // can't alch, skip
                    }

                    if (canAlch && Rs2Player.getRealSkillLevel(Skill.MAGIC) > 54 && Rs2Magic.canCast(HIGH_LEVEL_ALCHEMY)) {

                        if (Rs2Inventory.contains(STEEL_KITESHIELD)) {
                            Rs2Magic.alch("Steel kiteshield");
                        } else if (Rs2Inventory.contains(BLACK_SQ_SHIELD)) {
                            Rs2Magic.alch("Black sq shield");
                        } else if (Rs2Inventory.contains(MITHRIL_SWORD)) {
                            Rs2Magic.alch("Mithril sword");
                        }

                        Rs2Player.waitForXpDrop(Skill.MAGIC, 10000, false);
                    }
                }

                // if at the safe spot attack the moss giant and run to the safespot
                if (isAtSafeSpot() && !Rs2Player.isInteracting() && desired2093Exists()) {
                    attackMossGiant();
                }

                if (isAtSafeSpot() && move) {
                    walkFastCanvas(SAFESPOT1);
                    sleepUntil(() -> !Rs2Player.isMoving());
                    if (getHealthPercentage() < 81) {
                        sleep(900,1400);
                    Rs2Player.eatAt(80);
                    Rs2Player.eatAt(80);}
                    Rs2Npc.interact(MOSS_GIANT_2093,"Attack");
                    sleepUntil(() -> Rs2Player.getInteracting() != null);
                    if (Rs2Player.getInteracting() == null) {
                        Microbot.log("We are not interacting with anything, trying to attack again");
                        Rs2Npc.interact(MOSS_GIANT_2093,"Attack");
                    }
                    move = false;
                    iveMoved = true;
                }

                if (adjacentToSafeSpot1()) {
                    walkFastCanvas(SAFESPOT);
                }


                if (!Rs2Player.isInteracting() && iveMoved && isAtSafeSpot1() && !isAnyMossGiantInteractingWithMe()) {
                    System.out.println("ive moved is false");
                    iveMoved = false;
                }

                if (Rs2Player.isInteracting() && isAtSafeSpot1() && isAnyMossGiantInteractingWithMe() && safeSpot1Attack) {
                    walkFastCanvas(SAFESPOT);
                    iveMoved = false;
                }

                if (config.buryBones() && !Rs2Player.isInteracting()) {
                    List<Rs2ItemModel> bones = Rs2Inventory.getBones();

                    for (Rs2ItemModel bone : bones) {
                        Microbot.status = "Burying " + bone.getName();

                        if (Rs2Inventory.hover1(bone)) {
                            Rs2Player.waitForAnimation(); // wait for bury to start
                            sleep(200, 250); // simulate reaction time
                            sleep(400, 600); // short delay before next
                        } else {
                            Microbot.log("Failed to hover over: " + bone.getName());
                            Microbot.log("running normal inventory interact bury");
                            Rs2Inventory.interact(bone, "bury");
                            sleep(200, 250); // simulate reaction time
                            sleep(400, 600); // short delay before next
                        }
                    }
                }

                // Check if any players are near and hop if there are
                if (SAFE_ZONE_AREA.contains(Rs2Player.getWorldLocation())) {playersCheck();}

                if (mossKillerPlugin.isSuperJammed())
                {if (Rs2Player.getInteracting() == null) {
                    Microbot.log("We are not interacting with anything, trying to attack again");
                    Rs2Npc.interact(MOSS_GIANT_2093,"Attack");
                    sleep(2600);}
                    Microbot.log("we are still possibly super jammed, gonna go and bank or just end loop");
                    if (mossKillerPlugin.isSuperJammed()) doBankingLogic();}

                //Microbot.autoLockInDefXp(Microbot.activeAccountIndex);

                long endTime = System.currentTimeMillis();
                long totalTime = endTime - startTime;
                Microbot.log("Total time for loop " + totalTime);

            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
        return true;
    }

    public static boolean stopPlayerMonitorPluginBlocking() {
        PlayerMonitorPlugin playerMonitorPlugin = (PlayerMonitorPlugin) Microbot.getPluginManager().getPlugins().stream()
                .filter(plugin -> plugin.getClass().getName().equals(PlayerMonitorPlugin.class.getName()))
                .findFirst()
                .orElse(null);

        if (playerMonitorPlugin == null) {
            Microbot.log("player monitor plugin not found. Cannot stop.");
            return false;
        }

        CountDownLatch latch = new CountDownLatch(1);
        Microbot.getClientThread().invokeLater(() -> {
            try {
                Microbot.getPluginManager().setPluginEnabled(playerMonitorPlugin, false);
                Microbot.stopPlugin(playerMonitorPlugin);
            } catch (Exception e) {
                Microbot.log("Error stopping plugin playermonitor", e);
            } finally {
                latch.countDown();
            }
        });

        try {
            latch.await(); // Wait until the plugin is stopped
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }

        return true;
    }


    public static boolean startPlayerMonitorPluginBlocking() {
        PlayerMonitorPlugin playerMonitorPlugin = (PlayerMonitorPlugin) Microbot.getPluginManager().getPlugins().stream()
                .filter(plugin -> plugin.getClass().getName().equals(PlayerMonitorPlugin.class.getName()))
                .findFirst()
                .orElse(null);

        if (playerMonitorPlugin == null) {
            Microbot.log("player monitor plugin not found. Cannot start.");
            return false;
        }

        CountDownLatch latch = new CountDownLatch(1);
        Microbot.getClientThread().invokeLater(() -> {
            try {
                Microbot.getPluginManager().setPluginEnabled(playerMonitorPlugin, true);
                Microbot.startPlugin(playerMonitorPlugin);
            } catch (Exception e) {
                Microbot.log("Error starting plugin playermonitor", e);
            } finally {
                latch.countDown();
            }
        });

        try {
            latch.await(); // Wait until the plugin is started
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }

        return true;
    }

    /**
     * Stops the MossKiller Plugin if it's currently active.
     *
     * @return true if the plugin was successfully stopped, false if it was not found or not active.
     */
    public static boolean stopMossKillerPlugin() {
        // Attempt to retrieve the autologin from the active plugin list
        MossKillerPlugin mossKillerPlugin = (MossKillerPlugin) Microbot.getPluginManager().getPlugins().stream()
                .filter(plugin -> plugin.getClass().getName().equals(MossKillerPlugin.class.getName()))
                .findFirst()
                .orElse(null);

        Microbot.getClientThread().invokeLater(() -> {
            try {
                assert mossKillerPlugin != null;
                Microbot.getPluginManager().setPluginEnabled(mossKillerPlugin, false);
                Microbot.stopPlugin(mossKillerPlugin);
            } catch (Exception e) {
                Microbot.log("Error stopping plugin moss killer", e);
            }
        });
        return true;
    }

    private boolean equipmentIsPrepared() {
        if (config.attackStyle() == MAGIC && Rs2Equipment.isWearing(STAFF_OF_FIRE)) {
            return true;
        }

        return config.attackStyle() == RANGE
                && ((Rs2Equipment.isWearing(MAPLE_SHORTBOW) || Rs2Equipment.isWearing(WILLOW_SHORTBOW) || Rs2Equipment.isWearing(OAK_SHORTBOW)) || Rs2Equipment.isWearing(SHORTBOW))
                && ((Rs2Equipment.isWearing(MITHRIL_ARROW) || Rs2Inventory.contains(MITHRIL_ARROW))
                || (Rs2Equipment.isWearing(BRONZE_ARROW) || Rs2Inventory.contains(BRONZE_ARROW)));
    }

    /// /// reserved for anti-pk logic /// ///

    //int interactingTicks = 0;

    /*public void dealWithPker() {
        if (Rs2Npc.getNpcsForPlayer() == null
                && Rs2Player.getPlayersInCombatLevelRange() != null) {
            Player localPlayer = Rs2Player.getLocalPlayer();
            for (Rs2PlayerModel p : Rs2Player.getPlayersInCombatLevelRange()) {
                if (p != null && p != localPlayer && p.getInteracting() == localPlayer) {
                    interactingTicks++;
                    break;
                }
            }

            if (interactingTicks > 3) {
                fired = true;
            }
        } else {
            interactingTicks = 0; // reset if not in combat
        }
    }*/


    private boolean isInMossGiantArea() {
        return SAFE_ZONE_AREA.contains(Rs2Player.getWorldLocation());
    }

    private boolean attackMossGiant() {
        Rs2NpcModel mossGiant = Rs2Npc.getNpc(MOSS_GIANT_2093);

        if (mossGiant != null && Rs2Player.getWorldLocation() != null) {
            double distance = mossGiant.getWorldLocation().distanceTo(Rs2Player.getWorldLocation());
            System.out.println("Distance to moss giant: " + distance);
        }

        if (!mossGiant.isDead() && isInMossGiantArea() && !Rs2Player.isAnimating()) {
            Rs2Camera.turnTo(mossGiant);
            Rs2Npc.interact(mossGiant, "attack");
            sleep(100,300);
            sleepUntil(Rs2Player::isAnimating);
            if (Rs2Player.isInCombat() && !flag1) {
                if (!Rs2Player.waitForXpDrop(DEFENCE, 4000)) {
                    sleep(1200);
                    Rs2Tab.switchToCombatOptionsTab();
                    sleep(2900);
                    Rs2Combat.setAttackStyle(COMBAT_STYLE_FOUR);
                }
                flag1 = true;
            }

            if (Rs2Player.isInteracting()) {
                if (!isAtSafeSpot() && !iveMoved && !move && !safeSpot1Attack)
                    sleep(600,900);
                if (!isAtSafeSpot()) walkFastCanvas(SAFESPOT);
                sleepUntil(this::isAtSafeSpot);
                sleepUntil(() -> !Rs2Npc.isMoving(mossGiant));
                if (!mossGiant.isDead() && !Rs2Player.isInteracting()) {
                    Rs2Npc.attack(MOSS_GIANT_2093);
                }
            }

            return true;
        }

        return false;
    }

    private void playersCheck() {
        if(!mossKillerScript.getNearbyPlayers(14).isEmpty()){
            if (Rs2Player.getPlayersInCombatLevelRange() != null) {
                Rs2Player.logout();
            }

            if(playerCounter > 15) {
                sleep(10000, 15000);
                int world = Login.getRandomWorld(false, null);
                if(world == 301){
                    return;
                }
                boolean isHopped = Microbot.hopToWorld(world);
                sleepUntil(() -> isHopped, 5000);
                if (!isHopped) return;
                playerCounter = 0;
                int randomThreshold = (int) Rs2Random.truncatedGauss(0, 5, 1.5); // Adjust mean and deviation as needed
                if (randomThreshold > 3) {
                    Rs2Inventory.open();
                }
                return;
            }
            playerCounter++;
        } else {
            playerCounter = 0;
        }
    }

    public boolean isAnyMossGiantInteractingWithMe() {
        Stream<Rs2NpcModel> mossGiantStream = Rs2Npc.getNpcs("Moss giant");

        if (mossGiantStream == null) {
            System.out.println("No Moss Giants found (Stream is null).");
            return false;
        }

        var player = Rs2Player.getLocalPlayer();
        if (player == null) {
            System.out.println("Local player not found!");
            return false;
        }

        String playerName = player.getName();
        System.out.println("Local Player Name: " + playerName);

        List<Rs2NpcModel> mossGiants = mossGiantStream.collect(Collectors.toList());

        for (Rs2NpcModel mossGiant : mossGiants) {
            if (mossGiant != null) {
                var interacting = mossGiant.getInteracting();
                String interactingName = interacting != null ? interacting.getName() : "None";

                System.out.println("Moss Giant interacting with: " + interactingName);

                if (interacting != null && interactingName.equals(playerName)) {
                    System.out.println("A Moss Giant is interacting with YOU!");
                    return true;
                }
            }
        }

        System.out.println("No Moss Giant is interacting with you.");
        return false;
    }

    private boolean itemsToLoot() {
        RS2Item[] items = Rs2GroundItem.getAllFromWorldPoint(5, SAFESPOT);
        System.out.println("is there anything to loot?");
        if (items.length == 0) return false;
        System.out.println("getting past return false");

        for (int lootItem : LOOT_LIST) {
            for (RS2Item item : items) {
                if (item.getItem().getId() == lootItem) {
                    return true; // Lootable item found
                }
            }
        }

        if (config.buryBones()) {
            for (RS2Item item : items) {
                if (item.getItem().getId() == BIG_BONES) {
                    return true;
                }
            }
        }

        if (config.alchLoot()) {
            for (int lootItem : ALCHABLES) {
                for (RS2Item item : items) {
                    if (item.getItem().getId() == lootItem) {
                        return true; // Lootable item found
                    }
                }
            }

        }

        return false; // No lootable items found
    }

    public boolean isAtSafeSpot() {
        WorldPoint playerPos = Rs2Player.getWorldLocation();
        return playerPos.equals(SAFESPOT);
    }

    public boolean isAtSafeSpot1() {
        WorldPoint playerPos = Rs2Player.getWorldLocation();
        return playerPos.equals(SAFESPOT1);
    }

    public boolean adjacentToSafeSpot1() {
        WorldPoint playerPos = Rs2Player.getWorldLocation();
        WorldPoint westOfSafeSpot = new WorldPoint(
                SAFESPOT1.getX() - 1,
                SAFESPOT1.getY(),
                SAFESPOT1.getPlane()
        );
        return playerPos.equals(westOfSafeSpot);
    }

    private void lootItems() {
        Microbot.status = "looting";
        if (Rs2Player.getInteracting() == null && !Rs2Player.isInCombat()) {
            System.out.println("entering loot items");
            RS2Item[] items = Rs2GroundItem.getAllFromWorldPoint(5, SAFESPOT);

            if (items == null || items.length == 0) {
                System.out.println("No items found to loot");
                return;
            }

            // Loot items from the predefined list
            for (RS2Item item : items) {
                if (Rs2Inventory.isFull()) {
                    System.out.println("Inventory full, stopping looting");
                    break;
                }

                int itemId = item.getItem().getId();
                boolean itemLooted = false;

                // Check regular loot items
                for (int lootItem : LOOT_LIST) {
                    if (itemId == lootItem) {
                        System.out.println("Looting regular item: " + itemId);
                        Rs2GroundItem.loot(lootItem);
                        int timeout = Rs2Random.between(1000,3000);
                        Rs2Inventory.waitForInventoryChanges(timeout);
                        sleep(50,250);
                        itemLooted = true;
                        break;
                    }
                }

                // If we already looted this item, continue to next item
                if (itemLooted) continue;

                if (Rs2GroundItem.loot("Coins", 119, 7)) {
                    Rs2Inventory.waitForInventoryChanges(3500);
                }

                for (int i = 0; i < 3; i++) {
                    if (Rs2GroundItem.loot("Mithril arrow", 4, 7)) {
                        Rs2Inventory.waitForInventoryChanges(3500);
                        sleep(50,250);
                    }
                }

                for (int i = 0; i < 3; i++) {
                    if (Rs2GroundItem.loot("Bronze arrow", 8, 7)) {
                        Rs2Inventory.waitForInventoryChanges(3500);
                        sleep(50,250);
                    }
                }



                // Handle alchables if enabled
                if (config.alchLoot() && !Rs2Inventory.isFull()) {
                    System.out.println("Checking for alchables, config.alchLoot() = " + config.alchLoot());
                    for (int lootItem : ALCHABLES) {
                        if (itemId == lootItem) {
                            System.out.println("Looting alchable: " + itemId);
                            Rs2GroundItem.loot(lootItem);
                            sleep(1000, 3000);
                            itemLooted = true;
                            break;
                        }
                    }
                }

                // If we already looted this item, continue to next item
                if (itemLooted) continue;

                // Handle bones separately if enabled
                if (config.buryBones() && !Rs2Inventory.isFull()) {
                    if (itemId == BIG_BONES) {
                        System.out.println("Handling bones: " + itemId);

                        boolean hasRunes = Rs2Inventory.contains(LAW_RUNE) && Rs2Inventory.contains(AIR_RUNE);
                        int magicLevel = Rs2Player.getRealSkillLevel(Skill.MAGIC);

                        // Telekinetic Grab requires level 33 Magic
                        if (hasRunes && magicLevel >= 33 && magicLevel < 55) {
                            if (Rs2Magic.canCast(MagicAction.TELEKINETIC_GRAB)) {
                                Microbot.log("Casting Telekinetic Grab on Big Bones");
                                Rs2Magic.cast(MagicAction.TELEKINETIC_GRAB);
                                sleep(2000, 4000);

                                // Interact to grab bones after casting
                                if (Rs2GroundItem.interact("Big bones", "Cast")) {
                                    sleep(2000, 4000);
                                } else {
                                    Microbot.log("Failed to interact with Big Bones after casting. Falling back to normal loot.");
                                    Rs2GroundItem.loot(BIG_BONES);
                                }
                            } else {
                                Microbot.log("Cannot cast Telekinetic Grab. Looting normally.");
                                Rs2GroundItem.loot(BIG_BONES);
                            }
                        } else {
                            Microbot.log("Missing runes or insufficient Magic level. Looting normally.");
                            Rs2GroundItem.loot(BIG_BONES);
                        }

                        sleep(1000, 3000);
                    }
                }
            }
        }
        sleep(400,900);
    }

    private boolean desired2093Exists() {
        Stream<Rs2NpcModel> mossGiantsStream = getNpcs(MOSS_GIANT_2093);
        List<Rs2NpcModel> mossGiants = mossGiantsStream.collect(Collectors.toList());

        for (Rs2NpcModel mossGiant : mossGiants) {
            if (SAFE_ZONE_AREA.contains(mossGiant.getWorldLocation())) {
                return true;
            }
        }

        return false;
    }


    public void doMonkBankingLogic() {

        Microbot.log("Bank is open: " + isOpen());
        Rs2Bank.walkToBank();
        sleep(5000);
        if (!ShortestPathPlugin.isStartPointSet()) {handleAsynchWalk("Bank");
            sleepUntil(Rs2Bank::walkToBank1, 160000);}
        sleep(2000,5000);
        openBank1();
        Rs2Bank.depositAll();
        sleepUntil(Rs2Inventory::isEmpty);
        depositEquipment();
        sleepUntil(() -> !Rs2Equipment.isWearing());

        if (!Rs2Inventory.isEmpty()) {
            Rs2Bank.depositAll();
            sleepUntil(Rs2Inventory::isEmpty);
        }

        if (!Rs2Equipment.isWearing()) {
            depositEquipment();
            sleepUntil(() -> !Rs2Equipment.isWearing());
        }

        if (Rs2Bank.count(COINS) > 130000 && Rs2Player.getRealSkillLevel(Skill.MAGIC) < 33) {
            sleep(1000);
            Microbot.noWaterStaff = !Rs2Bank.hasItem(1383);
            if (Rs2Inventory.hasItem(MITHRIL_AXE) || Rs2Inventory.hasItem(BRONZE_AXE)) {
                Microbot.log("already got an axe, not withdrawing one");
            } else if (Rs2Player.getRealSkillLevel(WOODCUTTING) > 20) {
                if (Rs2Bank.hasItem(MITHRIL_AXE))
                    Rs2Bank.withdrawOne(MITHRIL_AXE);
                else if (Rs2Bank.hasItem(BRONZE_AXE))
                    Rs2Bank.withdrawOne(BRONZE_AXE);
            } else {
                if (Rs2Bank.hasItem(BRONZE_AXE))
                    Rs2Bank.withdrawOne(BRONZE_AXE);
            }
            Rs2Bank.withdrawAndEquip(MONKS_ROBE);
            Rs2Bank.withdrawAndEquip(MONKS_ROBE_TOP);
            Microbot.bonesToBananas = true;
            if (!Rs2Bank.closeBank()) {
                Microbot.log("bank wasn't open to be closed");
            }
        } else {
            if (Rs2Inventory.hasItem(MITHRIL_AXE) || Rs2Inventory.hasItem(BRONZE_AXE)) {
                Microbot.log("already got an axe, not withdrawing one");
            } else if (Rs2Player.getRealSkillLevel(WOODCUTTING) > 20) {
                if (Rs2Bank.hasItem(MITHRIL_AXE))
                    Rs2Bank.withdrawOne(MITHRIL_AXE);
                else if (Rs2Bank.hasItem(BRONZE_AXE))
                    Rs2Bank.withdrawOne(BRONZE_AXE);
            } else {
                if (Rs2Bank.hasItem(BRONZE_AXE))
                    Rs2Bank.withdrawOne(BRONZE_AXE);
            }
            Rs2Bank.withdrawAndEquip(MONKS_ROBE);
            Rs2Bank.withdrawAndEquip(MONKS_ROBE_TOP);
            sleep(1000);
            Microbot.herringTime = true;}
    }

    public void handleAsynchWalk(String walkName) {
        scheduledFuture = scheduledExecutorService.schedule(() -> {
            try {
                Microbot.log("Entered Asynch Walking Thread");
                WorldPoint playerLocation = Rs2Player.getLocalPlayer().getWorldLocation();

                    switch (walkName) {
                        case "Bank":
                            Microbot.log("triggered walk to nearest bank");
                            isWalking = true;

                            // Async task to get nearest bank and start walking
                            CompletableFuture
                                    .supplyAsync(Rs2Bank::getNearestBank)
                                    .thenAccept(nearestBank -> {
                                        if (nearestBank != null) {
                                            Microbot.log("Walking to: " + nearestBank.name());
                                            ShortestPathPlugin.walkToViaPanel(Rs2Bank.getNearestBank().getWorldPoint());
                                        } else {
                                            Microbot.log("No nearby bank found.");
                                        }
                                        isWalking = false;
                                    })
                                    .exceptionally(ex -> {
                                        Microbot.log("Error finding nearest bank: " + ex.getMessage());
                                        isWalking = false;
                                        return null;
                                    });
                            break;

                        case "Start-up":
                            Microbot.log("Starting up scheduled future");
                            break;
                    }

                Microbot.log("Exiting Asynch Walking Thread");

            } catch (Exception ex) {
                Microbot.log("Exception in walking thread: " + ex.getMessage());
            }
        }, 600, TimeUnit.MILLISECONDS);
    }

    private void doBankingLogic() {
        int amuletId = config.rangedAmulet().getItemId();
        int torsoId = config.rangedTorso().getItemId();
        int chapsId = config.rangedChaps().getItemId();
        int capeId = config.cape().getItemId();

        Microbot.status = "banking";

        if (mossKillerPlugin.shouldHopWorld()) {
            Rs2Bank.closeBank();
            sleep(9200);
            Microbot.log("Player is dead! Hopping worlds...");
            sleepUntil(() -> !Rs2Player.isInCombat());
            sleep(4900);
            Rs2Player.logout();
            sleep(2900);
            mossKillerPlugin.resetWorldHopFlag(); // Reset after hopping
        }

        if (config.attackStyle() == RANGE) {
            if (Rs2Bank.isOpen()) {
                Rs2Bank.depositAll();
            }

            if (!Rs2Bank.isOpen()) {
                Rs2Bank.walkToBank();
                Rs2Equipment.unEquip(AMMO);
                Rs2Bank.walkToBankAndUseBank();
            }

            if (Rs2Bank.openBank()){
                sleep(6000);
                if (Rs2Bank.openBank()) {
                    sleep(2200,3200);
                    //if (Rs2Bank.hasWithdrawAsNote()) Rs2Bank.setWithdrawAsItem();
                    if ((Rs2Bank.hasItem(BRONZE_ARROW) && Rs2Bank.count(BRONZE_ARROW) < config.mithrilArrowAmount())
                            //|| (Rs2Bank.hasItem(MITHRIL_ARROW) && Rs2Bank.count(MITHRIL_ARROW) < config.mithrilArrowAmount())
                            || (!Rs2Bank.hasItem(BRONZE_ARROW) && !Rs2Bank.hasItem(MITHRIL_ARROW))
                    || (Rs2Player.getRealSkillLevel(RANGED) < 5 && Rs2Bank.count(SHORTBOW) < 5)) {

                        if (!Rs2Bank.hasItem(BRONZE_ARROW) && !Rs2Bank.hasItem(MITHRIL_ARROW)) {
                            noArrows = true;
                        }

                        //if (Rs2Bank.hasItem(MITHRIL_ARROW) && Rs2Bank.count(MITHRIL_ARROW) < config.mithrilArrowAmount()) {
                            //mithArrows = true;
                        //}

                        if (Rs2Bank.hasItem(BRONZE_ARROW) && Rs2Bank.count(BRONZE_ARROW) < config.mithrilArrowAmount()) {
                            bronzeArrows = true;
                        }
                        boolean buyOakShortbow = Rs2Player.getRealSkillLevel(RANGED) < 20 && (!Rs2Bank.hasItem(OAK_SHORTBOW) || Rs2Bank.count(OAK_SHORTBOW) < 5 || !Rs2Bank.hasItem(SHORTBOW));

                        Microbot.sellingGe = true;
                        Microbot.log("NEED RESTOCK ARROWS.");
                        Rs2Bank.walkToBank(BankLocation.GRAND_EXCHANGE);
                        Microbot.log("firing walk to via panel just incase");
                        sleep(500);
                        ShortestPathPlugin.walkToViaPanel(new WorldPoint(3164, 3487,0));
                        Microbot.log("sleeping up to 30 seconds to actually get moving");
                        sleepUntil(Rs2Player::isMoving, 30000);
                        Microbot.log("sleeping up to 180 seconds to arrive at ge (hopefully constantly moving)");
                        sleepUntil(() -> !Rs2Player.isMoving(), 300000);
                        Microbot.log("we've registered as stopped moving");
                        Microbot.log("sleeping up to 300 seconds for distance to ge to be less than 10");
                        sleepUntil(() ->
                                        Rs2Walker.getDistanceBetween(
                                                Rs2Player.getLocalPlayer().getWorldLocation(),
                                                new WorldPoint(3164, 3487, 0)
                                        ) < 10,
                                180000);
                        if (buyOakShortbow) {
                            if (Rs2Walker.getDistanceBetween(
                                            Rs2Player.getLocalPlayer().getWorldLocation(),
                                            new WorldPoint(3164, 3487, 0)
                                    ) > 10) {
                                Microbot.log("NEED RESTOCK BOW.");
                                Rs2Bank.walkToBank(BankLocation.GRAND_EXCHANGE);
                                Microbot.log("firing walk to via panel just incase");
                                sleep(500);
                                ShortestPathPlugin.walkToViaPanel(new WorldPoint(3164, 3487, 0));
                                Microbot.log("sleeping up to 30 seconds to actually get moving");
                                sleepUntil(Rs2Player::isMoving, 30000);
                                Microbot.log("sleeping up to 180 seconds to arrive at ge (hopefully constantly moving)");
                                sleepUntil(() -> !Rs2Player.isMoving(), 300000);
                                Microbot.log("we've registered as stopped moving");
                                Microbot.log("sleeping up to 300 seconds for distance to ge to be less than 10");
                                sleepUntil(() ->
                                                Rs2Walker.getDistanceBetween(
                                                        Rs2Player.getLocalPlayer().getWorldLocation(),
                                                        new WorldPoint(3164, 3487, 0)
                                                ) < 10,
                                        180000);
                            }
                            Rs2GrandExchange.openExchange();
                            sleep(5000,9000);
                            Rs2GrandExchange.collectAllToBank1();
                            sleep(5000,9000);
                            Rs2GrandExchange.buyItem("Oak shortbow", 1000, 10);
                            sleep(5000,9000);
                            Rs2GrandExchange.buyItemExact("Shortbow", 1000, 10);
                            sleep(5000,9000);
                            Rs2GrandExchange.collectAllToBank1();
                            sleep(2000,9000);
                        }
                        if (bronzeArrows && mithArrows) bronzeArrows = false;
                        if (bronzeArrows || mithArrows) handleGrandExchangeTransactions();
                        if (noArrows) handleGrandExchangeTransactions();
                        //if (mithArrows) handleGrandExchangeTransactionsMith();
                        Microbot.sellingGe = false;
                        //stopMossKillerPlugin(); // Stop script
                        return;
                    }

                    if (!Rs2Bank.hasItem(BRONZE_2H_SWORD) &&
                            !Rs2Bank.hasItem(IRON_2H_SWORD) &&
                            !Rs2Bank.hasItem(STEEL_2H_SWORD) &&
                            !Rs2Bank.hasItem(BLACK_2H_SWORD) &&
                            !Rs2Bank.hasItem(MITHRIL_2H_SWORD)) {
                        Microbot.sellingGe = true;
                        Microbot.log("NEEDS A 2H");
                        Rs2Bank.walkToBank(BankLocation.GRAND_EXCHANGE);
                        Microbot.log("Firing walk to via panel just incase");
                        sleep(500);
                        ShortestPathPlugin.walkToViaPanel(new WorldPoint(3164, 3487,0));
                        Microbot.log("sleeping up to 30 seconds to actually get moving");
                        sleepUntil(Rs2Player::isMoving, 30000);
                        Microbot.log("sleeping up to 180 seconds to arrive at ge (hopefully constantly moving)");
                        sleepUntil(() -> !Rs2Player.isMoving(), 300000);
                        Microbot.log("we've registered as stopped moving");
                        Microbot.log("sleeping up to 300 seconds for distance to ge to be less than 10");
                        sleepUntil(() ->
                                        Rs2Walker.getDistanceBetween(
                                                Rs2Player.getLocalPlayer().getWorldLocation(),
                                                new WorldPoint(3164, 3487, 0)
                                        ) < 10,
                                180000);
                        handleGrandExchangeTransactions2H();
                        Microbot.sellingGe = false;
                        return;
                    }
                }
            }
        }
        if (config.attackStyle() == MAGIC) {
            if (Rs2Widget.isWidgetVisible(24772680)) {Rs2Widget.clickWidget(24772680);}
            sleepUntil(() -> !Rs2Widget.isWidgetVisible(24772680), 60000);
            Microbot.log("Bank is open: " + isOpen());
            Rs2Bank.walkToBank();
            sleep(5000);
            if (!ShortestPathPlugin.isStartPointSet()) {handleAsynchWalk("Bank");
                sleepUntil(Rs2Bank::walkToBank1, 160000);}
            sleep(2000,5000);
            openBank1();
                if (Rs2Bank.openBank()) {
                    sleepUntil(Rs2Bank::openBank, 30000);
                    Rs2Bank.withdrawOne(MONKS_ROBE_TOP);
                    sleep(900,1200);
                    Rs2Bank.depositOne(LOGS);
                    Microbot.log("attempted to intializae bank");
                    Rs2Bank.depositOne(OAK_LOGS);
                    sleep(900,1200);
                    Rs2Bank.depositOne(MONKS_ROBE_TOP);
                    sleep(900,1200);
                    Rs2Bank.depositAll();
                    sleep(900,1200);
                    boolean missing = Rs2Bank.count(APPLE_PIE) < 16 ||
                            Rs2Bank.count(MIND_RUNE) < 750 ||
                            Rs2Bank.count(AIR_RUNE) < 1550 ||
                            !Rs2Bank.hasItem(STAFF_OF_FIRE);

                    if (missing && config.attackStyle().equals(MAGIC)) {
                        if (missingConsumablesOnce) {
                            Microbot.sellingGe = true;
                            Microbot.log("Missing required consumables again. Going GE to purchase more.");
                            Microbot.mossKillerOn = true;
                            //stopMossKillerPlugin();
                            Rs2Bank.closeBank();
                            Microbot.log("closed the bank interface incase we gonna teleport");
                            sleep(1800);
                            Rs2Bank.walkToBank(BankLocation.GRAND_EXCHANGE);
                            Microbot.log("Should of arrived at GE");
                            sleep(1900,2900);
                            Rs2Bank.openBank1();
                            sleep(2100,3400);
                            Rs2Bank.depositAll();
                            sleep(1900,2900);
                            if (Rs2Bank.hasBankItem("Coins", 100_000)) {
                                Microbot.log("You have at least 100k coins in the bank.");
                                moreThan100k = true;
                            } else {
                                Microbot.log("You have less than 100k coins in the bank.");
                                moreThan100k = false;
                            }
                            if (Microbot.getClient().getRealSkillLevel(Skill.MAGIC) < 55) {
                                Rs2Bank.withdrawAll(STEEL_BAR);
                                sleep(1400,4500);
                                Rs2Bank.withdrawAll(COSMIC_RUNE);
                                sleep(1400,4500);
                                Rs2Bank.withdrawAll(UNCUT_DIAMOND);
                                sleep(1400,4500);
                                Rs2Bank.withdrawAll(UNCUT_RUBY);
                                sleep(1400,4500);
                                Rs2Bank.withdrawAll(DEATH_RUNE);
                                sleep(1400,4500);
                                Rs2Bank.withdrawAll(STEEL_ARROW);
                            } else {
                                // Check if we have any actual alchable items (not counting nature runes)
                                if (hasAlchablesInInventory()) {
                                    Rs2Bank.closeBank();
                                    Microbot.log("Found alchables in inventory, starting alching...");
                                    alchAllItems();
                                } else {
                                    Microbot.log("No alchables in inventory");
                                    // No alchables in inventory, check bank
                                    int[] currentAlchables = getAlchables();
                                    boolean hasAlchablesInBank = false;

                                    for (int id : currentAlchables) {
                                        if (id != ItemID.NATURE_RUNE && Rs2Bank.hasItem(id)) {
                                            hasAlchablesInBank = true;
                                            break;
                                        }
                                    }

                                    if (hasAlchablesInBank) {
                                        Microbot.log("Found alchables in bank, banking...");

                                        if (!Rs2Bank.isOpen()) {
                                            Rs2Bank.openBank();
                                            sleepUntil(() -> Rs2Bank.isOpen(), 5000);
                                        }

                                        if (Rs2Bank.isOpen()) {
                                            Rs2Bank.withdrawAndEquip(ItemID.STAFF_OF_FIRE);
                                            sleep(900, 1200);

                                            Rs2Bank.withdrawX(ItemID.NATURE_RUNE, 28);
                                            sleep(900, 1200);

                                            Rs2Bank.withdrawItemsAll(currentAlchables);
                                            sleep(600, 1900);

                                            Rs2Bank.closeBank();
                                            sleepUntil(() -> !Rs2Bank.isOpen(), 2000);
                                            alchAllItems();
                                        }
                                    } else {
                                        Microbot.log("No alchables in bank - alching script complete!");
                                        sleep(600, 1900);
                                        Rs2Bank.openBank1();
                                        sleep(600, 1900);
                                        Rs2Bank.depositAll();
                                        sleep(600, 1900);
                                        depositEquipment();
                                        sleep(600, 1900);
                                        Rs2Bank.withdrawAll(STEEL_BAR);
                                        sleep(1400,4500);
                                        Rs2Bank.withdrawAll(COSMIC_RUNE);
                                        sleep(1400,4500);
                                        Rs2Bank.withdrawAll(UNCUT_DIAMOND);
                                        sleep(1400,4500);
                                        Rs2Bank.withdrawAll(UNCUT_RUBY);
                                        sleep(1400,4500);
                                        Rs2Bank.withdrawAll(DEATH_RUNE);
                                        sleep(1400,4500);
                                        Rs2Bank.withdrawAll(STEEL_ARROW);
                                        sleep(5000);
                                            List<Integer> keepList = Arrays.asList(
                                                    MONKROBETOP,
                                                    MAPLE_SHORTBOW,
                                                    SHORTBOW,
                                                    OAK_SHORTBOW,
                                                    WILLOW_SHORTBOW,
                                                    MITHRIL_ARROW,
                                                    RUNE_SCIMITAR,
                                                    RUNE_CHAINBODY,
                                                    MAPLE_LONGBOW,
                                                    STRENGTH_POTION4,
                                                    STRENGTH_POTION3,
                                                    STRENGTH_POTION2,
                                                    STRENGTH_POTION1,
                                                    ENERGY_POTION4,
                                                    ENERGY_POTION3,
                                                    ENERGY_POTION2,
                                                    ENERGY_POTION1,
                                                    FIRE_RUNE,
                                                    STUDDED_BODY,
                                                    SWORDFISH,
                                                    AMULET_OF_POWER,
                                                    MITHRIL_AXE,
                                                    MONKROBEBOTTOM,
                                                    TINDERBOX,
                                                    LEATHER_VAMBRACES,
                                                    LEATHER_BOOTS,
                                                    AMULET_OF_MAGIC,
                                                    STAFF_OF_FIRE,
                                                    STAFF_OF_AIR,
                                                    APPLE_PIE,
                                                    TEAM50_CAPE,
                                                    BRONZE_AXE,
                                                    COINS,
                                                    MIND_RUNE,
                                                    556, //air rune
                                                    LAW_RUNE,
                                                    BRONZE_2H_SWORD,
                                                    IRON_2H_SWORD,
                                                    STEEL_2H_SWORD,
                                                    BLACK_2H_SWORD,
                                                    MITHRIL_2H_SWORD,
                                                    BREAD,
                                                    COOKED_MEAT,
                                                    BURNT_MEAT,
                                                    BRONZE_PICKAXE,
                                                    303, //small fishing net
                                                    SPADE,
                                                    KNIFE,
                                                    HAMMER,
                                                    HALF_AN_APPLE_PIE,
                                                    CHAOS_RUNE,
                                                    EARTH_RUNE,
                                                    NATURE_RUNE,
                                                    STUDDED_CHAPS,
                                                    FISHING_BAIT,
                                                    FISHING_ROD,
                                                    BRONZE_ARROW,
                                                    MOSSY_KEY,
                                                    SNOWBALL,
                                                    BONES,
                                                    STAFF_OF_WATER
                                            );
                                            Microbot.log("Started withdrawallexcept");
                                            Rs2Bank.sellAllExceptKeepList(keepList, -90);
                                            Microbot.log("finished withdrawallexcept");
                                    }
                                }
                            }
                            sleep(1999,4400);
                            Rs2Bank.closeBank();
                            sleep(2000,5000);
                            Rs2GrandExchange.openExchange();
                            sleep(1900,2900);
                            Rs2GrandExchange.collectAllToBank1();
                            // Only sell loot list items at -10%
                            if (!Rs2Inventory.isEmpty()){
                            sellLoot(Arrays.stream(LOOT_LIST2).boxed().collect(Collectors.toList()), -20);}

                            Rs2GrandExchange.collectAllToBank();
                            /// hardwire a collect all to bank, and fire it once (like the hover, just hard-code on the canvas the collect button)
                            sleep(1900,2900);
                            if (!moreThan100k) {Rs2GrandExchange.buyItem("Mind rune", 4, 1750);
                            Microbot.log("attempted to buy 2x stock of mind runes");
                            sleep(1900,2900);
                            Rs2GrandExchange.buyItem("Air rune", 7, 3750);
                            Microbot.log("attempted to buy 2x stock of air runes");
                            sleep(1900,2900);
                            Rs2GrandExchange.buyItem("Apple pie", 100, 10);
                            Microbot.log("attempted to buy 20 Apple pie");
                            sleep(1900,2900);
                                Rs2GrandExchange.collectAllToBank();
                                sleep(1900,2900);
                            if (Rs2Player.getRealSkillLevel(ATTACK) > 14) {
                                Microbot.log("attempting to buy a bronze and an iron 2h sword");
                                Rs2GrandExchange.buyItem("Bronze 2h Sword", 250, 2);
                                sleep(2900);
                                Rs2GrandExchange.buyItem("Iron 2h Sword", 500, 2);
                                sleep(2900);
                            }
                            Rs2GrandExchange.collectAllToBank();
                            Microbot.log("collected to bank called");
                            sleep(2900);
                            sleep(2900);
                            Rs2GrandExchange.closeExchange();
                            sleep(2900);
                            Microbot.log("exchange window ordererd to close and now returning");
                            } else {
                                Rs2GrandExchange.openExchange();
                                sleep(1900,2900);
                                Rs2GrandExchange.collectAllToBank1();
                                /// hardwire a collect all to bank, and fire it once (like the hover, just hard-code on the canvas the collect button)
                                sleep(1900,2900);
                                    Rs2GrandExchange.buyItem("Mind rune", 4, 4000);
                                    Microbot.log("attempted to buy 5x stock of mind runes");
                                    sleep(1900,2900);
                                    Rs2GrandExchange.buyItem("Air rune", 7, 9000);
                                    Microbot.log("attempted to buy 5x stock of air runes");
                                    sleep(1900,2900);
                                    //Rs2GrandExchange.buyItem("Apple pie", 100, 20);
                                    //Microbot.log("attempted to buy 60 Apple pie");
                                    sleep(1900,2900);
                                    Rs2GrandExchange.collectAllToBank();
                                    sleep(1900,2900);
                                    if (Rs2Player.getRealSkillLevel(ATTACK) > 14) {
                                        Microbot.log("attempting to buy a bronze and an iron 2h sword");
                                        Rs2GrandExchange.buyItem("Bronze 2h Sword", 250, 5);
                                        sleep(2900);
                                        Rs2GrandExchange.buyItem("Iron 2h Sword", 500, 5);
                                        sleep(2900);
                                    }
                                    Rs2GrandExchange.collectAllToBank();
                                    Microbot.log("collected to bank called");
                                    sleep(2900);
                                    sleep(2900);
                                    Rs2GrandExchange.closeExchange();
                                    sleep(2900);
                                    Microbot.log("exchange window ordererd to close and now returning");
                                Microbot.sellingGe = false;
                            }
                            return;
                        } else {
                            Microbot.log("Missing required consumables in the bank. Skipping once.");
                            missingConsumablesOnce = true;
                            return;
                        }
                    }
                } else {
                    Rs2Bank.openBank();
                    sleep(10000);
                    return;
                }
            }
        Rs2Bank.depositAll();
        sleep(600,1900);

        // Withdraw required consumables
        if (Rs2Player.getRealSkillLevel(DEFENCE) < 30) {Rs2Bank.withdrawX(APPLE_PIE, 12);} else {Rs2Bank.withdrawX(APPLE_PIE, 8);}
        sleep(300);
        if (config.attackStyle() == MAGIC) {
            //if (!Microbot.accountSessionMap.get(Microbot.activeAccountIndex).isFreshie || Microbot.accountSessionMap.get(Microbot.activeAccountIndex).freshieRoundCount > 19) {
                Rs2Bank.withdrawX(MIND_RUNE, 750);
            sleep(300);
            Rs2Bank.withdrawX(AIR_RUNE, 1550);
            sleep(300);
            Rs2Bank.withdrawOne(LAW_RUNE);
            sleep(300);
        //} else {
               // Rs2Bank.withdrawX(MIND_RUNE, 250);
                ///Rs2Bank.withdrawX(AIR_RUNE, 550);
                //sleep(300);
               // Rs2Bank.withdrawOne(LAW_RUNE);
                //sleep(300);
         //   }

            // Check if equipped with necessary items
            if (!isWearingWithRequiredItems() && config.attackStyle().equals(MAGIC)) {
                if (Rs2Bank.openBank()) {
                    sleep(10000);
                    Rs2Bank.depositOne(LOGS);
                    Microbot.log("attempted to initialize bank in required equipment block");
                    Rs2Bank.depositOne(OAK_LOGS);

                    // Ensure all required equipment is in the bank before proceeding
                    boolean missingEquipment = !Rs2Bank.hasItem(AMULET_OF_MAGIC) ||
                            !Rs2Bank.hasItem(STAFF_OF_FIRE) ||
                            !Rs2Bank.hasItem(capeId);

                    if (missingEquipment) {
                        if (missingEquipmentOnce) {
                            Microbot.log("Missing required equipment again. Shutting down script.");
                            Microbot.mossKillerOn = true;
                            stopMossKillerPlugin();
                            return;
                        } else {
                            Microbot.log("Missing required equipment in the bank. Skipping once. Activating equipment.");
                            depositEquipment();
                            sleep(2000);
                            Rs2Bank.withdrawAndEquip(AMULET_OF_MAGIC);
                            sleep(2000);
                            Rs2Bank.withdrawAndEquip(STAFF_OF_FIRE);
                            sleep(2000);
                            if (Rs2Inventory.contains(BRONZE_2H_SWORD)) {
                                Rs2Bank.depositOne(BRONZE_2H_SWORD);
                                sleep(900,1200);
                            }
                            if (Rs2Inventory.contains(IRON_2H_SWORD)) {
                                Rs2Bank.depositOne(IRON_2H_SWORD);
                                sleep(900,1200);
                            }
                            Rs2Bank.withdrawAndEquip(capeId);
                            sleep(2000);
                            if (Rs2Equipment.isWearing(STAFF_OF_FIRE)) {return;}
                            sleep(2000);
                            missingEquipmentOnce = true;
                            return;
                        }
                    }

                    OutfitHelper.equipOutfit(OutfitHelper.OutfitType.NAKED_MAGE);
                    Rs2Bank.withdrawAndEquip(STAFF_OF_FIRE);
                    Rs2Bank.withdrawAndEquip(capeId);
                    sleep(600, 900);
                }
            }
        }
        if (config.attackStyle() == RANGE) {

            if (!isWearingWithRequiredItemsRange() || !equipmentIsPrepared()) {
                depositEquipment();
                if (!Rs2Equipment.isWearing()) {
                    if (Rs2Bank.hasItem(MITHRIL_ARROW)) Rs2Bank.withdrawX(MITHRIL_ARROW, config.mithrilArrowAmount());
                    if (Rs2Bank.hasItem(BRONZE_ARROW)) sleep(1000,2200); if (!Rs2Inventory.hasItem(MITHRIL_ARROW)) Rs2Bank.withdrawX(BRONZE_ARROW, config.mithrilArrowAmount());
                    sleep(400,800);
                    sleep(400,900);
                }

                int[] equipItems = {
                        amuletId,
                        torsoId,
                        chapsId,
                        capeId,
                        MAPLE_SHORTBOW,
                        LEATHER_VAMBRACES,
                        LEATHER_BOOTS
                };

                for (int itemId : equipItems) {
                    Rs2Bank.withdrawAndEquip(itemId);
                    sleepUntil(() -> Rs2Equipment.isWearing(itemId), 5000);
                }
                if (Rs2Player.getRealSkillLevel(RANGED) < 6) {
                    if (!Rs2Bank.withdrawAndEquip(SHORTBOW) || !Rs2Bank.hasItem(SHORTBOW))
                        sleep(5000);
                }
                if (Rs2Player.getRealSkillLevel(RANGED) < 31 && Rs2Player.getRealSkillLevel(RANGED) > 5) {
                    if (!Rs2Bank.withdrawAndEquip(WILLOW_SHORTBOW)) {Rs2Bank.withdrawAndEquip(OAK_SHORTBOW); sleep(1000,2000);}
                    sleepUntil(() -> Rs2Equipment.isWearing(WILLOW_SHORTBOW), 5000);
                } else {Rs2Bank.withdrawAndEquip(MAPLE_SHORTBOW); sleepUntil(() -> Rs2Equipment.isWearing(MAPLE_SHORTBOW), 5000); }

            }

            if (Rs2Inventory.contains(MITHRIL_ARROW) && (Rs2Inventory.contains(BRONZE_ARROW) || Rs2Equipment.isWearing(BRONZE_ARROW)))
            {Rs2Bank.closeBank(); sleep(4400,6600); Rs2Equipment.unEquip(AMMO); sleep (1300,4000); Rs2Inventory.drop(BRONZE_ARROW);
                Rs2Inventory.waitForInventoryChanges(5000);
                Rs2Inventory.drop(BRONZE_ARROW);
            sleep(1200,1800);
            Rs2Bank.openBank1();
            sleep(1200,1800);}
            if (!Rs2Inventory.contains(MITHRIL_ARROW)) {Rs2Bank.withdrawXAndEquip(MITHRIL_ARROW, config.mithrilArrowAmount());}
            sleep(1200,1800);
            if (!Rs2Inventory.contains(BRONZE_ARROW)) {Rs2Bank.withdrawXAndEquip(BRONZE_ARROW, config.mithrilArrowAmount());}
            sleep(1200,1800);

            /// ///// when we were using teleport varrock
            if (Rs2Player.getRealSkillLevel(Skill.MAGIC) > 32 && Rs2Player.getRealSkillLevel(Skill.MAGIC) < 36) {
                if (Rs2Bank.count(LAW_RUNE) > 5) {Rs2Bank.withdrawX(LAW_RUNE, 5);
                sleep(900,1700);
                Rs2Bank.withdrawX(AIR_RUNE,11);
                sleep(800,1200);}
                //Rs2Bank.withdrawOne(FIRE_RUNE);
                //sleep(400,800);
            }

        }

        if (config.alchLoot() && Rs2Player.getRealSkillLevel(Skill.MAGIC) > 54) {Rs2Bank.withdrawX(NATURE_RUNE, 6);
            if (config.attackStyle() == RANGE && Rs2Player.getRealSkillLevel(Skill.MAGIC) > 54) Rs2Bank.withdrawX(FIRE_RUNE,50);}

        if (Rs2Inventory.hasItem(MITHRIL_AXE) || Rs2Inventory.hasItem(BRONZE_AXE)) {
            Microbot.log("already got an axe, not withdrawing one");
        }
        else if (Rs2Player.getRealSkillLevel(WOODCUTTING) > 20) {
            if (Rs2Bank.hasItem(MITHRIL_AXE))
                Rs2Bank.withdrawOne(MITHRIL_AXE);
            else if (Rs2Bank.hasItem(BRONZE_AXE))
                Rs2Bank.withdrawOne(BRONZE_AXE);
        }
        else {
            if (Rs2Bank.hasItem(BRONZE_AXE))
                Rs2Bank.withdrawOne(BRONZE_AXE);
        }
        //Rs2Bank.withdrawOne(TINDERBOX);
        sleep(900);
        Rs2Bank.closeBank();
    }


    private boolean isWearingWithRequiredItems() {
        int capeId = config.cape().getItemId();
        // Check if player is wearing the required items
        return Rs2Equipment.isWearing(AMULET_OF_MAGIC)
                && Rs2Equipment.isWearing(STAFF_OF_FIRE)
                && Rs2Equipment.isWearing(capeId);
    }


    private boolean isWearingWithRequiredItemsRange() {
        int amuletId = config.rangedAmulet().getItemId();
        //int torsoId = config.rangedTorso().getItemId();
        //int chapsId = config.rangedChaps().getItemId();
        int capeId = config.cape().getItemId();
        return Rs2Equipment.isWearing(amuletId)
                //&& Rs2Equipment.isWearing(chapsId)
                && Rs2Equipment.isWearing(capeId)
                //&& Rs2Equipment.isWearing(torsoId)
                && Rs2Equipment.isWearing(LEATHER_BOOTS)
                && Rs2Equipment.isWearing(LEATHER_VAMBRACES);
    }

    private void handleGrandExchangeTransactions2H() {
        // Open bank and sell items not in keep list
        boolean buyBronze = false;
        Rs2Bank.openBank1();
        sleep(2100, 3400);
        Rs2Bank.depositAll();
        sleep(1900, 2900);
        if (Rs2Bank.count(COINS) < 12501){
            buyBronze = true;
        }

        List<Integer> keepList = Arrays.asList(
                MONKROBETOP,
                MAPLE_SHORTBOW,
                WILLOW_SHORTBOW,
                OAK_SHORTBOW,
                SHORTBOW,
                MITHRIL_ARROW,
                RUNE_SCIMITAR,
                RUNE_CHAINBODY,
                MAPLE_LONGBOW,
                STRENGTH_POTION4,
                STRENGTH_POTION3,
                STRENGTH_POTION2,
                STRENGTH_POTION1,
                ENERGY_POTION4,
                ENERGY_POTION3,
                ENERGY_POTION2,
                ENERGY_POTION1,
                FIRE_RUNE,
                STUDDED_BODY,
                SWORDFISH,
                AMULET_OF_POWER,
                MITHRIL_AXE,
                MONKROBEBOTTOM,
                TINDERBOX,
                LEATHER_VAMBRACES,
                LEATHER_BOOTS,
                AMULET_OF_MAGIC,
                AMULET_OF_POWER,
                STAFF_OF_FIRE,
                STAFF_OF_AIR,
                APPLE_PIE,
                TEAM50_CAPE,
                BRONZE_AXE,
                COINS,
                MIND_RUNE,
                556, //air rune
                LAW_RUNE,
                BRONZE_2H_SWORD,
                IRON_2H_SWORD,
                STEEL_2H_SWORD,
                BLACK_2H_SWORD,
                MITHRIL_2H_SWORD,
                BREAD,
                COOKED_MEAT,
                BURNT_MEAT,
                BRONZE_PICKAXE,
                303, //small fishing net
                SPADE,
                KNIFE,
                HAMMER,
                HALF_AN_APPLE_PIE,
                CHAOS_RUNE,
                EARTH_RUNE,
                NATURE_RUNE,
                STUDDED_CHAPS,
                FISHING_BAIT,
                FISHING_ROD,
                BRONZE_ARROW,
                MOSSY_KEY,
                SNOWBALL,
                BONES,
                STAFF_OF_WATER
        );

        Microbot.log("Started withdrawallexcept");
        Rs2Bank.sellAllExceptKeepList(keepList, -90);
        Microbot.log("Finished withdrawallexcept");
        sleep(1999, 4400);

        Rs2Bank.closeBank();
        sleep(2000, 5000);

        // Now handle GE transactions
        Rs2GrandExchange.openExchange();
        sleep(1900, 2900);
        Rs2GrandExchange.collectAllToBank1();
        sleep(1900, 2900);

        /// / this is old method of selling bryophyta loot, it can probably be removed consdiring new sellAllExceptKeepList method
        // Sell loot items
        if (!Rs2Inventory.isEmpty()) {
            sellLoot(Arrays.stream(LOOT_LIST2).boxed().collect(Collectors.toList()), -20);
        }

        Rs2GrandExchange.collectAllToBank();
        sleep(1900, 2900);

        if (Rs2Player.getRealSkillLevel(STRENGTH) < 2  && !buyBronze) {
            // equal chance to buy black or mithril or steel or bronze or iron 2h
            int randomChoice = Rs2Random.between(0, 4); // 0-4
            if (randomChoice == 0) {
                Rs2GrandExchange.buyItem("Bronze 2h Sword", 250, 5);
            } else if (randomChoice == 1) {
                Rs2GrandExchange.buyItem("Iron 2h Sword", 750, 5);
            } else if (randomChoice == 2) {
                Rs2GrandExchange.buyItem("Steel 2h Sword", 1000, 5);
            } else if (randomChoice == 3) {
                Rs2GrandExchange.buyItem("Black 2h Sword", 2000, 5);
            } else {
                Rs2GrandExchange.buyItem("Mithril 2h Sword", 2500, 5);
            }
            sleep(2900);
        }
        else if (Rs2Player.getRealSkillLevel(STRENGTH) < 3  && !buyBronze) {
            // equal chance to buy steel or bronze or iron 2h
            int randomChoice = Rs2Random.between(0, 2); // 0-2
            if (randomChoice == 0) {
                Rs2GrandExchange.buyItem("Bronze 2h Sword", 250, 5);
            } else if (randomChoice == 1) {
                Rs2GrandExchange.buyItem("Iron 2h Sword", 750, 5);
            } else {
                Rs2GrandExchange.buyItem("Steel 2h Sword", 1000, 5);
            }
            sleep(2900);
        }
        else if (Rs2Player.getRealSkillLevel(STRENGTH) >= 3  && !buyBronze) {
            // equal chance to buy bronze or iron 2h
            int randomChoice = Rs2Random.between(0, 1); // 0-1
            if (randomChoice == 0) {
                Rs2GrandExchange.buyItem("Bronze 2h Sword", 500, 5);
            } else {
                Rs2GrandExchange.buyItem("Iron 2h Sword", 750, 5);
            }
            sleep(2900);
        }

        if(buyBronze) {
            Rs2GrandExchange.buyItem("Bronze 2h Sword", 500, 5);
        }


        Rs2GrandExchange.collectAllToBank();
        sleep(1900, 2900);
        Rs2GrandExchange.closeExchange();
        sleep(2900);
        Microbot.log("GE transactions complete");
    }

    private void handleGrandExchangeTransactions() {
        // Open bank and sell items not in keep list
        Rs2Bank.openBank1();
        sleep(2100, 3400);
        Rs2Bank.depositAll();
        sleep(1900, 2900);
        int coinCount = Rs2Bank.count(COINS);
        int arrowPrice = 10; // Price per arrow
        int maxAffordable = coinCount / arrowPrice; // How many arrows we can afford


        List<Integer> keepList = Arrays.asList(
                MONKROBETOP,
                MAPLE_SHORTBOW,
                WILLOW_SHORTBOW,
                OAK_SHORTBOW,
                SHORTBOW,
                MITHRIL_ARROW,
                RUNE_SCIMITAR,
                RUNE_CHAINBODY,
                MAPLE_LONGBOW,
                STRENGTH_POTION4,
                STRENGTH_POTION3,
                STRENGTH_POTION2,
                STRENGTH_POTION1,
                ENERGY_POTION4,
                ENERGY_POTION3,
                ENERGY_POTION2,
                ENERGY_POTION1,
                FIRE_RUNE,
                STUDDED_BODY,
                SWORDFISH,
                AMULET_OF_POWER,
                MITHRIL_AXE,
                MONKROBEBOTTOM,
                TINDERBOX,
                LEATHER_VAMBRACES,
                LEATHER_BOOTS,
                AMULET_OF_MAGIC,
                AMULET_OF_POWER,
                STAFF_OF_FIRE,
                STAFF_OF_AIR,
                APPLE_PIE,
                TEAM50_CAPE,
                BRONZE_AXE,
                COINS,
                MIND_RUNE,
                556, //air rune
                LAW_RUNE,
                BRONZE_2H_SWORD,
                IRON_2H_SWORD,
                STEEL_2H_SWORD,
                BLACK_2H_SWORD,
                MITHRIL_2H_SWORD,
                BREAD,
                COOKED_MEAT,
                BURNT_MEAT,
                BRONZE_PICKAXE,
                303, //small fishing net
                SPADE,
                KNIFE,
                HAMMER,
                HALF_AN_APPLE_PIE,
                CHAOS_RUNE,
                EARTH_RUNE,
                NATURE_RUNE,
                STUDDED_CHAPS,
                FISHING_BAIT,
                FISHING_ROD,
                BRONZE_ARROW,
                MOSSY_KEY,
                SNOWBALL,
                BONES,
                STAFF_OF_WATER
        );

        Microbot.log("Started withdrawallexcept");
        Rs2Bank.sellAllExceptKeepList(keepList, -90);
        Microbot.log("Finished withdrawallexcept");
        sleep(1999, 4400);

        Rs2Bank.closeBank();
        sleep(2000, 5000);

        // Now handle GE transactions
        Rs2GrandExchange.openExchange();
        sleep(1900, 2900);
        Rs2GrandExchange.collectAllToBank1();
        sleep(1900, 2900);

        /// / this is old method of selling bryophyta loot, it can probably be removed consdiring new sellAllExceptKeepList method
        // Sell loot items
        if (!Rs2Inventory.isEmpty()) {
            sellLoot(Arrays.stream(LOOT_LIST2).boxed().collect(Collectors.toList()), -20);
        }

        Rs2GrandExchange.collectAllToBank();
        sleep(1900, 2900);
        Rs2GrandExchange.closeExchange();
        sleep(2900,5000);
        Rs2Bank.openBank1();
        sleep(4000,6000);

        /// / couldn't we dynamically purchase considering the coinstack we have

        if (maxAffordable > 0) {
            // Buy as many as we can afford, up to a maximum of 2000
            int amountToBuy = Math.min(maxAffordable, 2000);
            Rs2Bank.closeBank();
            sleep(4300,9000);
            Rs2GrandExchange.buyItem("Bronze arrow", arrowPrice, amountToBuy);
            Microbot.log("Attempted to buy " + amountToBuy + " bronze arrows for " + (amountToBuy * arrowPrice) + "gp");
            sleep(1900, 2900);
            Rs2GrandExchange.collectAllToBank();
            sleep(1900, 2900);
        } else {
            Microbot.log("Not enough coins to buy bronze arrows");
        }
        sleep(1900, 2900);
        Rs2GrandExchange.collectAllToBank();
        sleep(1900, 2900);

        Rs2GrandExchange.collectAllToBank();
        sleep(1900, 2900);
        Rs2GrandExchange.closeExchange();
        sleep(2900);
        Microbot.log("GE transactions complete");
        noArrows = false;
        sleep(1000);
        bronzeArrows = false;
    }

    private void handleGrandExchangeTransactionsMith() {
        // Open bank and sell items not in keep list
        Rs2Bank.openBank1();
        sleep(2100, 3400);
        Rs2Bank.depositAll();
        sleep(1900, 2900);
        int coinCount = Rs2Bank.count(COINS);
        int arrowPrice = 20; // Price per arrow
        int maxAffordable = coinCount / arrowPrice; // How many arrows we can afford


        List<Integer> keepList = Arrays.asList(
                MONKROBETOP,
                MAPLE_SHORTBOW,
                WILLOW_SHORTBOW,
                OAK_SHORTBOW,
                SHORTBOW,
                MITHRIL_ARROW,
                RUNE_SCIMITAR,
                RUNE_CHAINBODY,
                MAPLE_LONGBOW,
                STRENGTH_POTION4,
                STRENGTH_POTION3,
                STRENGTH_POTION2,
                STRENGTH_POTION1,
                ENERGY_POTION4,
                ENERGY_POTION3,
                ENERGY_POTION2,
                ENERGY_POTION1,
                FIRE_RUNE,
                STUDDED_BODY,
                SWORDFISH,
                AMULET_OF_POWER,
                MITHRIL_AXE,
                MONKROBEBOTTOM,
                TINDERBOX,
                LEATHER_VAMBRACES,
                LEATHER_BOOTS,
                AMULET_OF_MAGIC,
                AMULET_OF_POWER,
                STAFF_OF_FIRE,
                STAFF_OF_AIR,
                APPLE_PIE,
                TEAM50_CAPE,
                BRONZE_AXE,
                COINS,
                MIND_RUNE,
                556, //air rune
                LAW_RUNE,
                BRONZE_2H_SWORD,
                IRON_2H_SWORD,
                STEEL_2H_SWORD,
                BLACK_2H_SWORD,
                MITHRIL_2H_SWORD,
                BREAD,
                COOKED_MEAT,
                BURNT_MEAT,
                BRONZE_PICKAXE,
                303, //small fishing net
                SPADE,
                KNIFE,
                HAMMER,
                HALF_AN_APPLE_PIE,
                CHAOS_RUNE,
                EARTH_RUNE,
                NATURE_RUNE,
                STUDDED_CHAPS,
                FISHING_BAIT,
                FISHING_ROD,
                BRONZE_ARROW,
                MOSSY_KEY,
                SNOWBALL,
                BONES,
                STAFF_OF_WATER
        );

        Microbot.log("Started withdrawallexcept");
        Rs2Bank.sellAllExceptKeepList(keepList, -90);
        Microbot.log("Finished withdrawallexcept");
        sleep(1999, 4400);

        Rs2Bank.closeBank();
        sleep(2000, 5000);

        // Now handle GE transactions
        Rs2GrandExchange.openExchange();
        sleep(1900, 2900);
        Rs2GrandExchange.collectAllToBank1();
        sleep(1900, 2900);

        /// / this is old method of selling bryophyta loot, it can probably be removed consdiring new sellAllExceptKeepList method
        // Sell loot items
        if (!Rs2Inventory.isEmpty()) {
            sellLoot(Arrays.stream(LOOT_LIST2).boxed().collect(Collectors.toList()), -20);
        }

        Rs2GrandExchange.collectAllToBank();
        sleep(1900, 2900);
        Rs2GrandExchange.closeExchange();
        sleep(2900,5000);
        Rs2Bank.openBank1();
        sleep(4000,6000);

        /// / couldn't we dynamically purchase considering the coinstack we have

        if (maxAffordable > 0) {
            // Buy as many as we can afford, up to a maximum of 2000
            int amountToBuy = Math.min(maxAffordable, 2000);
            Rs2Bank.closeBank();
            sleep(4300,9000);
            Rs2GrandExchange.buyItem("Mithril arrow", arrowPrice, amountToBuy);
            Microbot.log("Attempted to buy " + amountToBuy + " mithril arrows for " + (amountToBuy * arrowPrice) + "gp");
            sleep(1900, 2900);
            Rs2GrandExchange.collectAllToBank();
            sleep(1900, 2900);
        } else {
            Microbot.log("Not enough coins to buy mithril arrows");
        }
        sleep(1900, 2900);
        Rs2GrandExchange.collectAllToBank();
        sleep(1900, 2900);

        Rs2GrandExchange.collectAllToBank();
        sleep(1900, 2900);
        Rs2GrandExchange.closeExchange();
        sleep(2900);
        Microbot.log("GE transactions complete");
        mithArrows = false;
    }

    private void prepareSchedulerStart() {
        Rs2Bank.walkToBank();
        Rs2Bank.openBank();
        sleepUntil(Rs2Bank::isOpen);
        Rs2Bank.depositAll();
        depositEquipment();
        Rs2Bank.closeBank();
        Rs2Bank.walkToBank(BankLocation.FEROX_ENCLAVE);
    }

    private boolean isInventoryPreparedMage() {
        return Rs2Inventory.hasItemAmount(MIND_RUNE, 15) &&
                Rs2Inventory.hasItemAmount(AIR_RUNE, 30) &&
                (Rs2Inventory.hasItemAmount(APPLE_PIE, 1) || Rs2Inventory.hasItemAmount(HALF_AN_APPLE_PIE, 1));
    }

    private boolean isInventoryPreparedArcher() {
        return Rs2Inventory.hasItemAmount(APPLE_PIE, 1) || Rs2Inventory.hasItemAmount(HALF_AN_APPLE_PIE, 1);
    }

        public boolean hasAlchablesInInventory() {
            for (int id : getAlchables()) {
                if (Rs2Inventory.contains(id)) {
                    return true;
                }
            }
            return false;
        }

        public boolean alchAllItems() {
            Microbot.log("Starting alching process...");

            int[] currentAlchables = getAlchables();
            int magicLevel = Microbot.getClient().getRealSkillLevel(Skill.MAGIC);
            MagicAction alchSpell = magicLevel >= 55 ? MagicAction.HIGH_LEVEL_ALCHEMY : MagicAction.LOW_LEVEL_ALCHEMY;

            Microbot.log("Magic level: " + magicLevel + ", using spell: " + alchSpell.getName());

            // For each alchable item type
            for (int id : currentAlchables) {
                // Skip nature runes - we don't want to alch our casting supplies!
                if (id == ItemID.NATURE_RUNE) {
                    continue;
                }

                Microbot.log("Processing item ID: " + id);

                // Check if item exists before entering while loop
                if (!Rs2Inventory.contains(id)) {
                    Microbot.log("Item ID " + id + " not found in inventory, skipping...");
                    continue;
                }

                int itemCount = Rs2Inventory.count(id);
                Microbot.log("Found " + itemCount + " of item ID " + id + " in inventory");

                // Keep alching this specific item type until none left in inventory
                int attempts = 0;
                while (Rs2Inventory.contains(id) && attempts < 50) {
                    attempts++;

                    Microbot.log("Alching attempt " + attempts + " for item ID " + id);

                    Rs2ItemModel itemToAlch = Rs2Inventory.get(id);

                    if (itemToAlch != null) {
                        // Double check we're not trying to alch nature runes
                        if (itemToAlch.getId() == ItemID.NATURE_RUNE) {
                            Microbot.log("Skipping nature rune!");
                            break;
                        }

                        Microbot.log("Got item model: " + itemToAlch.getName() + " (Count: " + Rs2Inventory.count(itemToAlch.getId()) + ")");

                        // Check if we have nature runes
                        int natureRunes = Rs2Inventory.count(ItemID.NATURE_RUNE);
                        Microbot.log("Nature runes in inventory: " + natureRunes);
                        if (natureRunes <= 0) {
                            Microbot.log("No nature runes! Cannot alch.");
                            return false;
                        }

                        Microbot.log("Attempting to cast " + alchSpell.getName());
                        if (Rs2Magic.cast(alchSpell)) {
                            Microbot.log("Cast successful, waiting...");
                            sleep(200, 400);

                            Microbot.log("Interacting with item...");
                            Rs2Inventory.interact(itemToAlch, "cast");

                            Microbot.log("Waiting for alch to complete...");
                            sleep(1200, 1800);

                            // Check if the item count actually decreased
                            int newCount = Rs2Inventory.count(id);
                            Microbot.log("Item count after alch - Before: " + itemCount + ", After: " + newCount);
                            itemCount = newCount;

                        } else {
                            Microbot.log("Failed to cast " + alchSpell.getName());
                            return false;
                        }
                    } else {
                        Microbot.log("Could not get item model for ID " + id);
                        break;
                    }
                }

                if (attempts >= 50) {
                    Microbot.log("Hit attempt limit for item ID " + id);
                }

                Microbot.log("Finished processing item ID " + id);
            }

            Microbot.log("Finished alching all items in inventory");
            return true;
        }

    @Override
    public void shutdown() {
        super.shutdown();
        fired = false;
        move = false;
        iveMoved = false;
        missingConsumablesOnce = false;
        missingEquipmentOnce = false;
        isWalking = false;
        setItOnce = false;
        moreThan100k = false;
        flag1 = false;
        noArrows = false;
        mithArrows = false;
        bronzeArrows = false;

    }
}