package database.entity;

import database.entity.annotations.Column;
import database.entity.annotations.Id;
import database.entity.annotations.Table;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Table("room_npcs")
public abstract class RoomNpcEntity extends UpdateableEntity {
	@Id
	@Column("floor")
	protected final int floor;
	
	@Id
	@Column("tile_id")
	protected final int tileId;
	
	@Column("npc_id")
	protected final Integer npcId;
}
