package database.entity.update;

import lombok.AllArgsConstructor;
import lombok.Builder;
import database.entity.UpdateableEntity;
import database.entity.annotations.Column;
import database.entity.annotations.Id;
import database.entity.annotations.Operation;
import database.entity.annotations.Table;

@Builder
@AllArgsConstructor
@Table("player")
@Operation("update")
public class UpdatePlayerEntity extends UpdateableEntity {
	@Id
	private Integer id;
	
	@Column("tile_id")
	private Integer tileId;
	
	@Column("attack_style_id")
	private Integer attackStyleId;
	
	@Column("floor")
	private Integer floor;
}
