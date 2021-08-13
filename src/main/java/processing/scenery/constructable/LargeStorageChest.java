package processing.scenery.constructable;

import database.dto.ConstructableDto;

public class LargeStorageChest extends StorageChest {

	public LargeStorageChest(int floor, int tileId, int lifetimeTicks, ConstructableDto dto) {
		super(floor, tileId, lifetimeTicks, dto);
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
