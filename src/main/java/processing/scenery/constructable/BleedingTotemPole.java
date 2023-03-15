package processing.scenery.constructable;

import database.dto.ConstructableDto;
import responses.ResponseMaps;

public class BleedingTotemPole extends RadialConstructable {
	private static final int PROC_TIMER = 6;

	public BleedingTotemPole(int floor, int tileId, int lifetimeTicks, ConstructableDto dto, boolean onHousingTile) {
		super(floor, tileId, lifetimeTicks, dto, onHousingTile, 3);
	}
	
	public void processConstructable(int tickId, ResponseMaps responseMaps) {
		if (tickId % PROC_TIMER == 0) {
			getPlayersOnAffectingTileIds()
				.forEach(player -> player.incrementHitpoints(responseMaps));
		}
	}

}
