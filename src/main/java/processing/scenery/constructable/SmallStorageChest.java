package processing.scenery.constructable;

import database.dto.ConstructableDto;

public class SmallStorageChest extends StorageChest {
	public SmallStorageChest(int floor, int tileId, int lifetimeTicks, ConstructableDto dto, boolean onHousingTile) {
		super(floor, tileId, lifetimeTicks, dto, onHousingTile);
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
