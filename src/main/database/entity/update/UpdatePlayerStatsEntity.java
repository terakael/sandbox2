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
