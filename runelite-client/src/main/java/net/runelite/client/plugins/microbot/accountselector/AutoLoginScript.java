package net.runelite.client.plugins.microbot.accountselector;

import net.runelite.api.GameState;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.MossKiller.MossKillerPlugin;
import net.runelite.client.plugins.microbot.MossKiller.WildySaferScript;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.breakhandler.BreakHandlerScript;
import net.runelite.client.plugins.microbot.shortestpath.ShortestPathPlugin;
import net.runelite.client.plugins.microbot.util.security.Login;

import java.util.concurrent.TimeUnit;

public class AutoLoginScript extends Script {

    public boolean run(AutoLoginConfig autoLoginConfig) {
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!super.run()) return;
                if (Microbot.shutDownAutoLogin) {Microbot.shutDownAutoLogin = false; shutdown();}
                if (BreakHandlerScript.isBreakActive()){if (Microbot.superJammed) {Microbot.superJammed = false;} return;}
                if (Microbot.superJammed) {
                    sleep(30000);
                    if (Microbot.superJammed && !BreakHandlerScript.isBreakActive()) {
                    WildySaferScript.stopMossKillerPlugin();
                    sleep(15000);
                    startMossKillerPlugin();
                    Microbot.superJammed = false;
                    sleep(150000);}
                }

                if (Microbot.getClient().getGameState() == GameState.LOGIN_SCREEN) {
                    if (autoLoginConfig.useRandomWorld()) {
                        new Login(Login.getRandomWorld(autoLoginConfig.isMember()));
                    } else {
                        new Login(autoLoginConfig.world());
                    }
                    sleep(5000);
                }


            } catch (Exception ex) {
                Microbot.logStackTrace(this.getClass().getSimpleName(), ex);
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
        return true;
    }

    public static boolean stopShortestPathPlugin() {
        ShortestPathPlugin shortestPathPlugin = (ShortestPathPlugin) Microbot.getPluginManager().getPlugins().stream()
                .filter(plugin -> plugin.getClass().getName().equals(ShortestPathPlugin.class.getName()))
                .findFirst()
                .orElse(null);

        if (shortestPathPlugin == null) {
            Microbot.log("ShortestPathPlugin not found");
            return false;
        }

        Microbot.getClientThread().invokeLater(() -> {
            try {
                Microbot.getPluginManager().setPluginEnabled(shortestPathPlugin, false);
                Microbot.stopPlugin(shortestPathPlugin);
            } catch (Exception e) {
                Microbot.log("Error stopping ShortestPath", e);
            }
        });
        return true;
    }

    public static void startShortestPathPlugin() {
        ShortestPathPlugin shortestPathPlugin = (ShortestPathPlugin) Microbot.getPluginManager().getPlugins().stream()
                .filter(plugin -> plugin.getClass().getName().equals(ShortestPathPlugin.class.getName()))
                .findFirst()
                .orElse(null);

        Microbot.getClientThread().invokeLater(() -> {
            try {
                Microbot.getPluginManager().setPluginEnabled(shortestPathPlugin, true);
                Microbot.startPlugin(shortestPathPlugin);
            } catch (Exception e) {
                Microbot.log("Error starting ShortestPath", e);
            }
        });
    }

    public static void startMossKillerPlugin() {
        // 1. Retrieve the plugin instance safely using the class type
        MossKillerPlugin mossKillerPlugin = (MossKillerPlugin) Microbot.getPluginManager().getPlugins().stream()
                .filter(MossKillerPlugin.class::isInstance)
                .findFirst()
                .orElse(null);

        if (mossKillerPlugin == null) {
            Microbot.log("MossKillerPlugin not found in PluginManager.");
            return;
        }

        // 2. Invoke the startup on the Client Thread
        Microbot.getClientThread().invokeLater(() -> {
            try {
                if (!Microbot.getPluginManager().isPluginEnabled(mossKillerPlugin)) {
                    Microbot.getPluginManager().setPluginEnabled(mossKillerPlugin, true);
                    Microbot.startPlugin(mossKillerPlugin);
                }
            } catch (Exception e) {
                Microbot.log("Error starting MossKillerPlugin: " + e.getMessage());
            }
        });
    }
}
