package database.entity;

import database.entity.annotations.Column;
import database.entity.annotations.Id;
import database.entity.annotations.Table;
import lombok.AllArgsConstructor;

@Table("player_artisan_blocked_tasks")
@AllArgsConstructor
public abstract class PlayerArtisanBlockedTaskEntity extends UpdateableEntity {
	@Id
	@Column("player_id")
	protected Integer playerId;
	
	@Column("item1")
	protected Integer item1;
	
	@Column("item2")
	protected Integer item2;
	
	@Column("item3")
	protected Integer item3;
	
	@Column("item4")
	protected Integer item4;
	
	@Column("item5")
	protected Integer item5;
}
