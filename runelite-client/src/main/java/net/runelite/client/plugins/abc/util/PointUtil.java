package net.runelite.client.plugins.abc.util;

import java.awt.*;

public class PointUtil {
    public static Point toAwtPoint(net.runelite.api.Point point) {
        return new Point(point.getX(), point.getY());
    }
}
