package net.runelite.client.plugins.microbot.example;

import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2MiniMap;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class ExampleOverlay extends OverlayPanel {

    private static final int GRID_RADIUS = 5;

    // 🔧 Offsets you can adjust while testing
    // 🔧 Offsets you can adjust while testing
    private int OFFSET_X = 0;
    private int OFFSET_Y = 0;

    @Inject
    public ExampleOverlay() {
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
    }


    @Override
    public Dimension render(Graphics2D g)
    {
        if (Microbot.getClient().getLocalPlayer() == null)
            return null;

        WorldPoint playerLoc = Rs2Player.getWorldLocation();
        Map<Point, java.awt.Point> gridPoints = new HashMap<>();

        for (int dx = -GRID_RADIUS; dx <= GRID_RADIUS; dx++)
        {
            for (int dy = -GRID_RADIUS; dy <= GRID_RADIUS; dy++)
            {
                WorldPoint wp = new WorldPoint(
                        playerLoc.getX() + dx,
                        playerLoc.getY() + dy,
                        playerLoc.getPlane()
                );

                Point p = Rs2MiniMap.worldToMinimap(wp);

                if (p == null || !Rs2MiniMap.isPointInsideMinimap(p))
                    continue;

                // 🔧 Apply your experimental offsets here
                int adjustedX = p.getX() + OFFSET_X;
                int adjustedY = p.getY() + OFFSET_Y;

                Point adjusted = new Point(adjustedX, adjustedY);
                gridPoints.put(adjusted, new java.awt.Point(dx, dy));

                g.setColor(Color.GREEN);
                g.fillOval(adjustedX - 2, adjustedY - 2, 4, 4);
                g.setColor(Color.WHITE);
                g.setFont(new Font("Arial", Font.PLAIN, 9));
                g.drawString(dx + "," + dy, adjustedX + 4, adjustedY);
            }
        }

        // Draw connecting lines to visualize grid skew
        g.setStroke(new BasicStroke(1f));
        for (Map.Entry<Point, java.awt.Point> a : gridPoints.entrySet())
        {
            for (Map.Entry<Point, java.awt.Point> b : gridPoints.entrySet())
            {
                if (Math.abs(a.getValue().x - b.getValue().x) + Math.abs(a.getValue().y - b.getValue().y) == 1)
                {
                    g.setColor(a.getValue().x != b.getValue().x ? new Color(0, 128, 255, 120) : new Color(255, 50, 50, 120));
                    g.drawLine(a.getKey().getX(), a.getKey().getY(), b.getKey().getX(), b.getKey().getY());
                }
            }
        }

        // Draw player marker
        g.setColor(Color.CYAN);
        Point playerPoint = Rs2MiniMap.worldToMinimap(playerLoc);
        if (playerPoint != null)
        {
            int px = playerPoint.getX() + OFFSET_X;
            int py = playerPoint.getY() + OFFSET_Y;
            g.fillOval(px - 3, py - 3, 6, 6);
            g.drawString("Player", px + 6, py);
        }

        // 🔍 Label offsets for quick feedback
        g.setColor(Color.YELLOW);
        g.setFont(new Font("Arial", Font.BOLD, 12));
        g.drawString("OffsetX: " + OFFSET_X + " | OffsetY: " + OFFSET_Y, 20, 20);

        return null;
    }

    public void adjustOffset(int dx, int dy) {  // Remove 'static'
        OFFSET_X += dx;
        OFFSET_Y += dy;
        Microbot.log("Minimap offset adjusted → X: " + OFFSET_X + ", Y: " + OFFSET_Y);
    }
}
