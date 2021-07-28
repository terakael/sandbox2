package database.entity;

import database.entity.annotations.Column;
import database.entity.annotations.Id;
import database.entity.annotations.Table;
import lombok.AllArgsConstructor;

@Table("player_base_animations")
@AllArgsConstructor
public abstract class PlayerBaseAnimationsEntity extends UpdateableEntity {
	@Id
	@Column("player_id")
	protected Integer playerId;
	
	@Id
	@Column("player_part_id")
	protected Integer playerPartId;
	
	@Column("base_animation_id")
	protected Integer baseAnimationId;
	
	@Column("color")
	protected Integer color;
}
