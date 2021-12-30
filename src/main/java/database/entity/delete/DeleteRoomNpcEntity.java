package database.entity.delete;

import database.entity.RoomNpcEntity;
import database.entity.annotations.Operation;

@Operation("delete")
public class DeleteRoomNpcEntity extends RoomNpcEntity {

	public DeleteRoomNpcEntity(int floor, int tileId, Integer npcId) {
		super(floor, tileId, npcId);
	}

}
