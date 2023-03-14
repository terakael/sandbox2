package database.entity.insert;

import database.entity.HousingConstructableStorageEntity;
import database.entity.annotations.Operation;

@Operation("insert")
public class InsertHousingConstructableStorageEntity extends HousingConstructableStorageEntity {

	public InsertHousingConstructableStorageEntity(int floor, int tileId, int playerId, int constructableId, int slot, int itemId, int count, int charges) {
		super(floor, tileId, playerId, constructableId, slot, itemId, count, charges);
	}

}
