package main.processing.scenery.constructable;

import main.database.dto.ConstructableDto;

public class LargeStorageChest extends StorageChest {

	public LargeStorageChest(int floor, int tileId, ConstructableDto dto) {
		super(floor, tileId, dto);
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
