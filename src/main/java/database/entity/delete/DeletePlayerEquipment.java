package database.entity.delete;

import lombok.Builder;
import database.entity.PlayerEquipment;
import database.entity.annotations.Operation;

@Operation("delete")
public class DeletePlayerEquipment extends PlayerEquipment {
	@Builder
	public DeletePlayerEquipment(Integer playerId, Integer equipmentId, Integer slot) {
		super(playerId, equipmentId, slot);
	}

}
