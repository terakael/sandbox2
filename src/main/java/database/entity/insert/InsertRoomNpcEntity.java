package database.entity.insert;

import database.entity.RoomNpcEntity;
import database.entity.annotations.Operation;

@Operation("insert")
public class InsertRoomNpcEntity extends RoomNpcEntity {

	public InsertRoomNpcEntity(int floor, int tileId, Integer npcId) {
		super(floor, tileId, npcId);
	}

}
