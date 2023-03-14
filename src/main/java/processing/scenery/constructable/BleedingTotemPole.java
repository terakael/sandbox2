package processing.scenery.constructable;

import database.dto.ConstructableDto;
import processing.managers.LocationManager;
import responses.ResponseMaps;

public class BleedingTotemPole extends Constructable {
	private static final int PROC_TIMER = 6;

	public BleedingTotemPole(int floor, int tileId, int lifetimeTicks, ConstructableDto dto, boolean onHousingTile) {
		super(floor, tileId, lifetimeTicks, dto, onHousingTile);
	}
	
	public void processConstructable(int tickId, ResponseMaps responseMaps) {
		if (tickId % PROC_TIMER == 0) {
			LocationManager.getLocalPlayers(floor, tileId, 3).forEach(player -> player.incrementHitpoints(responseMaps));
		}
	}

}
