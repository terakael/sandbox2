package database.entity;

import database.entity.annotations.Column;
import database.entity.annotations.Id;
import database.entity.annotations.Table;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Table("housing_constructables")
public class HousingConstructableEntity extends UpdateableEntity {
	@Id
	@Column("floor")
	protected final Integer floor;
	
	@Id
	@Column("tile_id")
	protected final Integer tileId;
	
	@Column("constructable_id")
	protected final Integer constructableId;
}
