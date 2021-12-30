package database.entity.insert;

import database.entity.RoomSceneryEntity;
import database.entity.annotations.Operation;

@Operation("insert")
public class InsertRoomSceneryEntity extends RoomSceneryEntity {

	public InsertRoomSceneryEntity(int floor, int tileId, Integer sceneryId) {
		super(floor, tileId, sceneryId);
	}

}
