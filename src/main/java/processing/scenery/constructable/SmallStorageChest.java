package processing.scenery.constructable;

import database.dto.ConstructableDto;

public class SmallStorageChest extends StorageChest {
	public SmallStorageChest(int floor, int tileId, int lifetimeTicks, ConstructableDto dto) {
		super(floor, tileId, lifetimeTicks, dto);
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
