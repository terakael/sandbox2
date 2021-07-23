package database.entity.update;

import lombok.Builder;
import database.entity.UpdateableEntity;
import database.entity.annotations.Column;
import database.entity.annotations.Id;
import database.entity.annotations.Operation;
import database.entity.annotations.Table;

@Builder
@Table("player_storage")
@Operation("update")
public class UpdatePlayerStorageEntity extends UpdateableEntity {
	@Id
	@Column("player_id")
	private Integer playerId;
	
	@Id
	@Column("storage_id")
	private Integer storageId;
	
	@Id
	@Column("slot")
	private Integer slot;
	
	@Column("item_id")
	private Integer itemId;
	
	@Column("count")
	private Integer count;
	
	@Column("charges")
	private Integer charges;
}
