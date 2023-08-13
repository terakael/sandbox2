package processing.scenery.constructable;

import database.dto.ConstructableDto;
import responses.ResponseMaps;

public class SmallStorageChest extends StorageChest {
	public SmallStorageChest(int playerId, int floor, int tileId, int lifetimeTicks, ConstructableDto dto, boolean onHousingTile, ResponseMaps responseMaps) {
		super(playerId, floor, tileId, lifetimeTicks, dto, onHousingTile, responseMaps);
	}

	@Override
	public int getMaxSlots() {
		return 8;
	}

	@Override
	public String getName() {
		return "small storage";
	}
	
}
