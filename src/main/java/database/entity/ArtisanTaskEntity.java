package database.entity;

import database.entity.annotations.Column;
import database.entity.annotations.Id;
import database.entity.annotations.Table;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Table("player_artisan_task")
public abstract class ArtisanTaskEntity extends UpdateableEntity {
	@Id
	@Column("player_id")
	private final Integer playerId;
	
	@Column("assigned_master_id")
	private final Integer assignedMasterId;
	
	@Column("item_id")
	private final Integer itemId;
	
	@Column("assigned_amount")
	private final Integer assignedAmount;
	
	@Column("handed_in_amount")
	private final Integer handedInAmount;
	
	@Column("total_tasks")
	private final Integer totalTasks;
	
	@Column("total_points")
	private final Integer totalPoints;
}
