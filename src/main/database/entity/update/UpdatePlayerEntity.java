package main.database.entity.update;

import lombok.AllArgsConstructor;
import lombok.Builder;
import main.database.entity.UpdateableEntity;
import main.database.entity.annotations.Column;
import main.database.entity.annotations.Id;
import main.database.entity.annotations.Operation;
import main.database.entity.annotations.Table;

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
