package database.entity;

import database.entity.annotations.Column;
import database.entity.annotations.Id;
import database.entity.annotations.Table;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Table("room_scenery")
public abstract class RoomSceneryEntity extends UpdateableEntity {
	@Id
	@Column("floor")
	protected final int floor;
	
	@Id
	@Column("tile_id")
	protected final int tileId;
	
	@Column("scenery_id")
	protected final Integer sceneryId;
}
