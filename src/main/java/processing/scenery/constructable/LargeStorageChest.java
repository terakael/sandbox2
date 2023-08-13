package processing.scenery.constructable;

import database.dto.ConstructableDto;
import responses.ResponseMaps;

public class LargeStorageChest extends StorageChest {

	public LargeStorageChest(int playerId, int floor, int tileId, int lifetimeTicks, ConstructableDto dto, boolean onHousingTile, ResponseMaps responseMaps) {
		super(playerId, floor, tileId, lifetimeTicks, dto, onHousingTile, responseMaps);
	}

	@Override
	public int getMaxSlots() {
		return 16;
	}

	@Override
	public String getName() {
		return "large storage";
	}

}
