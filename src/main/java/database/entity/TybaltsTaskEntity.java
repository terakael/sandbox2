package database.entity;

import lombok.AllArgsConstructor;
import database.entity.annotations.Column;
import database.entity.annotations.Id;
import database.entity.annotations.Table;

@AllArgsConstructor
@Table("player_tybalts_tasks")
public abstract class TybaltsTaskEntity extends UpdateableEntity {
	@Id
	@Column("player_id")
	private Integer playerId;
	
	@Column("task_id")
	private Integer taskId;
	
	@Column("progress1")
	private Integer progress1;
	
	@Column("progress2")
	private Integer progress2;
	
	@Column("progress3")
	private Integer progress3;
	
	@Column("progress4")
	private Integer progress4;
}
