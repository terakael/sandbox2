package processing.scenery.constructable;

import database.dto.ConstructableDto;
import processing.managers.LocationManager;
import responses.ResponseMaps;

public class HolyTotemPole extends Constructable {
	private static final int PROC_TIMER = 3;

	public HolyTotemPole(int floor, int tileId, int lifetimeTicks, ConstructableDto dto) {
		super(floor, tileId, lifetimeTicks, dto);
	}

	public void processConstructable(int tickId, ResponseMaps responseMaps) {
		if (tickId % PROC_TIMER == 0)
			LocationManager.getLocalPlayers(floor, tileId, 3).forEach(player -> player.incrementPrayerPoints(responseMaps));
	}
}
