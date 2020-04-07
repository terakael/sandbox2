package main.database.entity.delete;

import lombok.Builder;
import main.database.entity.PlayerEquipment;
import main.database.entity.annotations.Operation;

@Operation("delete")
public class DeletePlayerEquipment extends PlayerEquipment {
	@Builder
	public DeletePlayerEquipment(Integer playerId, Integer equipmentId, Integer slot) {
		super(playerId, equipmentId, slot);
	}

}
