package net.runelite.client.plugins.microbot.shortestpath;

import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.util.walker.Rs2MiniMap;
import net.runelite.client.ui.overlay.Overlay;

import java.awt.*;

public class MinimapOverlay extends Overlay
{

	@Override
	public Dimension render(Graphics2D graphics)
	{

		Shape minimapClip = Rs2MiniMap.getMinimapClipArea();

		Widget minimapWidget = Rs2MiniMap.getMinimapDrawWidget();

		if (minimapWidget != null)
		{
			graphics.setColor(Color.RED);
			graphics.draw(minimapWidget.getBounds());
		}

		if (minimapClip != null)
		{
			graphics.setColor(Color.GREEN);
			graphics.draw(minimapClip);
		}

		return null;
	}
}
