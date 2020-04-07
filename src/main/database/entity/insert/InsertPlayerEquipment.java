package main.database.entity.insert;

import lombok.Builder;
import main.database.entity.PlayerEquipment;
import main.database.entity.annotations.Operation;

@Operation("insert")
public class InsertPlayerEquipment extends PlayerEquipment {
	@Builder
	public InsertPlayerEquipment(Integer playerId, Integer equipmentId, Integer slot) {
		super(playerId, equipmentId, slot);
	}
}
