package database.entity.delete;

import database.entity.RoomGroundTextureEntity;
import database.entity.annotations.Operation;

@Operation("delete")
public class DeleteRoomGroundTextureEntity extends RoomGroundTextureEntity {

	public DeleteRoomGroundTextureEntity(int floor, int tileId, Integer groundTextureId) {
		super(floor, tileId, groundTextureId);
	}

}
