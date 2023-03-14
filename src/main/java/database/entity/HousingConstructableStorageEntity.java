package database.entity;

import database.entity.annotations.Column;
import database.entity.annotations.Id;
import database.entity.annotations.Table;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Table("housing_constructable_storage")
public class HousingConstructableStorageEntity extends UpdateableEntity {
	@Id
	@Column("floor")
	protected final Integer floor;
	
	@Id
	@Column("tile_id")
	protected final Integer tileId;
	
	@Id
	@Column("player_id")
	protected final Integer playerId; // futureproofing - multiple players can store their own shit in the same chest
	
	@Column("constructable_id")
	protected final Integer constructableId;
	
	@Id
	@Column("slot")
	protected final Integer slot;
	
	@Column("item_id")
	protected final Integer itemId;
	
	@Column("count")
	protected final Integer count;
	
	@Column("charges")
	protected final Integer charges;
}
