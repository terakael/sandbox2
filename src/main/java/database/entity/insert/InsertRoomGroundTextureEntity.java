package database.entity.insert;

import database.entity.RoomGroundTextureEntity;
import database.entity.annotations.Operation;

@Operation("insert")
public class InsertRoomGroundTextureEntity extends RoomGroundTextureEntity {

	public InsertRoomGroundTextureEntity(int floor, int tileId, int groundTextureId) {
		super(floor, tileId, groundTextureId);
	}

}
