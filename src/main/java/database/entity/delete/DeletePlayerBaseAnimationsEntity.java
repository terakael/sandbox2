package database.entity.delete;

import database.entity.PlayerBaseAnimationsEntity;
import database.entity.annotations.Operation;
import lombok.Builder;

@Operation("delete")
public class DeletePlayerBaseAnimationsEntity extends PlayerBaseAnimationsEntity {

	@Builder
	public DeletePlayerBaseAnimationsEntity(Integer playerId, Integer playerPartId, Integer baseAnimationId, Integer color) {
		super(playerId, playerPartId, baseAnimationId, color);
	}

}
