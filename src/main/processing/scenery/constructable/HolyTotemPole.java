package main.processing.scenery.constructable;

import main.database.dto.ConstructableDto;
import main.processing.managers.LocationManager;
import main.responses.ResponseMaps;

public class HolyTotemPole extends Constructable {
	private static final int PROC_TIMER = 3;

	public HolyTotemPole(int floor, int tileId, ConstructableDto dto) {
		super(floor, tileId, dto);
	}

	public void processConstructable(int tickId, ResponseMaps responseMaps) {
		if (tickId % PROC_TIMER == 0)
			LocationManager.getLocalPlayers(floor, tileId, 3).forEach(player -> player.incrementPrayerPoints(responseMaps));
	}
}
