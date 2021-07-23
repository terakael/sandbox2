package database.entity;

import lombok.AllArgsConstructor;
import database.entity.annotations.Column;
import database.entity.annotations.Id;
import database.entity.annotations.Table;

@AllArgsConstructor
@Table("player_artisan_tasks")
public abstract class ArtisanTaskEntity extends UpdateableEntity {
	@Id
	@Column("player_id")
	private Integer playerId;
	
	@Column("item_id")
	private Integer itemId;
	
	@Column("progress1")
	private Integer progress1;
	
	@Column("progress2")
	private Integer progress2;
	
	@Column("progress3")
	private Integer progress3;
	
	@Column("progress4")
	private Integer progress4;
	
	@Column("progress5")
	private Integer progress5;
	
	@Column("progress6")
	private Integer progress6;
	
	@Column("progress7")
	private Integer progress7;
	
	@Column("progress8")
	private Integer progress8;
	
	@Column("progress9")
	private Integer progress9;
}
