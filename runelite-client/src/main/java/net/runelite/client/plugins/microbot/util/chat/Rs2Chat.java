package net.runelite.client.plugins.microbot.util.chat;

import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.plugins.microbot.Microbot;

import java.awt.*;

import static net.runelite.client.plugins.microbot.util.Global.sleep;

public class Rs2Chat {

    public static void clickAll() {
        clickTab(WidgetInfo.CHATBOX_TAB_ALL);
    }

    public static void clickGame() {
        clickTab(WidgetInfo.CHATBOX_TAB_GAME);
    }

    public static void clickPublic() {
        clickTab(WidgetInfo.CHATBOX_TAB_PUBLIC);
    }

    public static void clickPrivate() {
        clickTab(WidgetInfo.CHATBOX_TAB_PRIVATE);
    }

    public static void clickClan() {
        clickTab(WidgetInfo.CHATBOX_TAB_CLAN);
    }

    public static void clickTrade() {
        clickTab(WidgetInfo.CHATBOX_TAB_TRADE);
    }

    /**
     * Toggles to a given chat tab, waits briefly, then goes back to All.
     */
    public static void toggleToTabThenBack(WidgetInfo tabToToggle) {
        clickTab(tabToToggle);
        sleep(500, 800); // small delay
        clickAll();
        Microbot.log("Toggled " + tabToToggle.name() + " -> All");
    }


    private static void clickTab(WidgetInfo widgetInfo) {
        Widget tab = Microbot.getClient().getWidget(widgetInfo);
        if (tab != null && tab.getBounds() != null) {
            Microbot.getMouse().click(tab.getBounds());
            Microbot.log("Clicked chat tab: " + widgetInfo.name());
        } else {
            Microbot.log("Chat tab widget not found: " + widgetInfo.name());
        }
    }
}