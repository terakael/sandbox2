package database.entity;

import lombok.AllArgsConstructor;
import database.entity.annotations.Column;
import database.entity.annotations.Id;
import database.entity.annotations.Table;

@AllArgsConstructor
@Table("player_artisan_task_breakdown")
public abstract class ArtisanTaskBreakdownEntity extends UpdateableEntity {
	@Id
	@Column("player_id")
	private Integer playerId;
	
	@Id
	@Column("item_id")
	private Integer itemId;
	
	@Column("amount")
	private Integer amount;
}
