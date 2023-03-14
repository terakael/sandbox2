package database.entity.insert;

import database.entity.HousingConstructableEntity;
import database.entity.annotations.Operation;

@Operation("insert")
public class InsertHousingConstructableEntity extends HousingConstructableEntity {

	public InsertHousingConstructableEntity(int floor, int tileId, int constructableId) {
		super(floor, tileId, constructableId);
	}
	
}
