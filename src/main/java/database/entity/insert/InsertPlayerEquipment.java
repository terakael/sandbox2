package database.entity.insert;

import lombok.Builder;
import database.entity.PlayerEquipment;
import database.entity.annotations.Operation;

@Operation("insert")
public class InsertPlayerEquipment extends PlayerEquipment {
	@Builder
	public InsertPlayerEquipment(Integer playerId, Integer equipmentId, Integer slot) {
		super(playerId, equipmentId, slot);
	}
}
