package net.runelite.client.plugins.microbot.BoL;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("BoL")
public interface BoLConfig extends Config {
    // avoid logging out
    @ConfigItem(
            keyName = "neverLogOut",
            name = "Never log out",
            description = "Must have logout timer plugin turned off"
    )
    default boolean neverLogout() {
        return false;
    }

    @ConfigItem(
            keyName = "eatPie",
            name = "eatPie",
            description = "eatPie when walking"
    )
    default boolean eatPie() {
        return false;
    }

    @ConfigItem(
            keyName = "Dialogues",
            name = "Dialogues",
            description = "every tick runs continue dialogues if there is dialogues"
    )
    default boolean dialogues() {
        return false;
    }


    @ConfigItem(
            keyName = "Dismiss random events",
            name = "Dismiss Random Events",
            description = "dismisses random events when not animating"
    )
    default boolean dismissRandom() {
        return false;
    }

}
