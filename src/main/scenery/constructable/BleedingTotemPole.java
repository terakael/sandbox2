package main.scenery.constructable;

import main.database.dto.ConstructableDto;
import main.processing.LocationManager;
import main.responses.ResponseMaps;

public class BleedingTotemPole extends Constructable {
	private static final int PROC_TIMER = 6;

	public BleedingTotemPole(int floor, int tileId, ConstructableDto dto) {
		super(floor, tileId, dto);
		// TODO Auto-generated constructor stub
	}
	
	public void processConstructable(int tickId, ResponseMaps responseMaps) {
		if (tickId % PROC_TIMER == 0) {
			LocationManager.getLocalPlayers(floor, tileId, 3).forEach(player -> player.incrementHitpoints(responseMaps));
		}
	}

}
