package main.database.entity;

import lombok.AllArgsConstructor;
import main.database.entity.annotations.Column;
import main.database.entity.annotations.Id;
import main.database.entity.annotations.Table;

@AllArgsConstructor
@Table("player_equipment")
public abstract class PlayerEquipment extends UpdateableEntity {
	@Id
	@Column("player_id")
	protected Integer playerId;
	
	@Id
	@Column("equipment_id")
	protected Integer equipmentId;
	
	@Column("slot")
	protected Integer slot;
}
