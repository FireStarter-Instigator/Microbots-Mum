package net.runelite.client.plugins.microbot.BoL.scripts;

import net.runelite.client.plugins.microbot.BlockingEvent;
import net.runelite.client.plugins.microbot.BlockingEventPriority;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

import static net.runelite.client.plugins.microbot.util.Global.sleepUntil;

public class DismissNpcEvent implements BlockingEvent {

    @Override
    public boolean validate() {
        Rs2NpcModel randomEventNPC = Rs2Npc.getRandomEventNPC();
        return Rs2Npc.hasLineOfSight(randomEventNPC);
    }

    @Override
    public boolean execute() {
        Rs2NpcModel randomEventNPC = Rs2Npc.getRandomEventNPC();

        sleepUntil(() -> !Rs2Player.isAnimating());
        Microbot.log("is it this always executing?");
        Rs2Npc.interact(randomEventNPC, "Dismiss");
        sleepUntil(() -> Rs2Npc.getRandomEventNPC() == null);
        return true;
    }

    @Override
    public BlockingEventPriority priority() {
        return BlockingEventPriority.LOWEST;
    }
}