package database.entity.delete;

import database.entity.RoomSceneryEntity;
import database.entity.annotations.Operation;

@Operation("delete")
public class DeleteRoomSceneryEntity extends RoomSceneryEntity {

	public DeleteRoomSceneryEntity(int floor, int tileId, Integer sceneryId) {
		super(floor, tileId, sceneryId);
	}

}
