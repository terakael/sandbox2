package database.entity.insert;

import database.entity.HousePetsEntity;
import database.entity.annotations.Operation;

@Operation("insert")
public class InsertHousePetsEntity extends HousePetsEntity {

	public InsertHousePetsEntity(int houseId, int petId, int floor) {
		super(houseId, petId, floor);
	}

}
