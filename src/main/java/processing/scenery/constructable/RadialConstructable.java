package processing.scenery.constructable;

import java.util.Set;
import java.util.stream.Collectors;

import database.dto.ConstructableDto;
import processing.PathFinder;
import processing.attackable.Player;
import processing.managers.LocationManager;
import utils.Utils;

// RadialConstructable is just a constructable that has a radius effect (Totem Poles etc)
public abstract class RadialConstructable extends Constructable {
	protected final int range;
	protected final Set<Integer> affectingTileIds;

	public RadialConstructable(int floor, int tileId, int lifetimeTicks, ConstructableDto dto, boolean onHousingTile, int range) {
		super(floor, tileId, lifetimeTicks, dto, onHousingTile);
		
		this.range = range;
		affectingTileIds = Utils.getLocalTiles(tileId, range).stream()
				.filter(e -> PathFinder.lineOfSightIsClear(floor, tileId, e, range))
				.collect(Collectors.toSet());
	}
	
	public Set<Player> getPlayersOnAffectingTileIds() {
		return LocationManager.getLocalPlayers(floor, tileId, range).stream()
				.filter(player -> affectingTileIds.contains(player.getTileId()))
				.collect(Collectors.toSet());
	}

}
