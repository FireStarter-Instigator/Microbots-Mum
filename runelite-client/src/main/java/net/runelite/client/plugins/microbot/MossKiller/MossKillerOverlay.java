package net.runelite.client.plugins.microbot.MossKiller;

import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;

import javax.inject.Inject;
import java.awt.*;

public class MossKillerOverlay extends OverlayPanel {

    @Inject
    MossKillerConfig config;
    private final MossKillerPlugin plugin;

    @Inject
    MossKillerOverlay(MossKillerPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
        setPosition(OverlayPosition.BOTTOM_LEFT);
        setNaughty();
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        try {
            panelComponent.setPreferredSize(new Dimension(120, 200));

            if (config.wildy()) {
                // Add the death counter for wilderness mode
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Deaths:")
                        .right(String.valueOf(plugin.getDeathCounter()))
                        .leftFont(new Font("Arial", Font.BOLD, 13))
                        .rightFont(new Font("Arial", Font.BOLD, 13))
                        .build());
            }

            if (!config.wildy()) {
                // Add the key counter for non-wilderness mode
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Keys:")
                        //.right(String.valueOf(plugin.getMossyKeyCounter()))
                        .leftFont(new Font("Arial", Font.BOLD, 13))
                        .rightFont(new Font("Arial", Font.BOLD, 13))
                        .build());
            }

            return super.render(graphics);

        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            return null;
        }
    }

}