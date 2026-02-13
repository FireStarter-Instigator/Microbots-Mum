package net.runelite.client.plugins.microbot.util.player;

import lombok.Getter;
import net.runelite.api.HeadIcon;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.client.plugins.microbot.util.ActorModel;

import java.awt.*;

@Getter
public class Rs2PlayerModel extends ActorModel implements Player
{

    private final Player player;

    public Rs2PlayerModel(final Player player)
    {
        super(player);
        this.player = player;
    }

    @Override
    public int getId()
    {
        return player.getId();
    }

    @Override
    public PlayerComposition getPlayerComposition()
    {
        return player.getPlayerComposition();
    }

    @Override
    public Polygon[] getPolygons() {
        return new Polygon[0];
    }

    // REMOVED: getPolygons() does not exist in the standard RuneLite Player interface.
    // If you need the convex hull, standard API uses getConvexHull() which is usually inherited from Actor.

    @Override
    public int getTeam()
    {
        return player.getTeam();
    }

    @Override
    public boolean isFriendsChatMember()
    {
        return player.isFriendsChatMember();
    }

    @Override
    public boolean isFriend()
    {
        return player.isFriend();
    }

    @Override
    public boolean isClanMember()
    {
        return player.isClanMember();
    }

    @Override
    public HeadIcon getOverheadIcon()
    {
        return player.getOverheadIcon();
    }

    @Override
    public int getSkullIcon()
    {
        return player.getSkullIcon();
    }

    @Override
    public void setSkullIcon(int i) {

    }

    @Override
    public int getFootprintSize() {
        return 0;
    }
}