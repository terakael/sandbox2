package database.entity;

import database.entity.annotations.Column;
import database.entity.annotations.Id;
import database.entity.annotations.Table;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@Table("player_npc_dialogue_entry_points")
public abstract class PlayerNpcDialogueEntryPointEntity extends UpdateableEntity {
	@Id
	@Column("player_id")
	protected Integer playerId;
	
	@Id
	@Column("npc_id")
	protected Integer npcId;
	
	@Column("point_id")
	protected Integer pointId;
}
