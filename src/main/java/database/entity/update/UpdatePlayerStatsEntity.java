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
@Table("player_stats")
@Operation("update")
public class UpdatePlayerStatsEntity extends UpdateableEntity {
	@Id
	@Column("player_id")
	private Integer playerId;
	
	@Id
	@Column("stat_id")
	private Integer statId;
	
	@Column("exp")
	private Double exp;
	
	@Column("relative_boost")
	private Integer relativeBoost;
}
