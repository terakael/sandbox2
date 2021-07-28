package database.entity.insert;

import database.entity.PlayerBaseAnimationsEntity;
import database.entity.annotations.Operation;
import lombok.Builder;

@Operation("insert")
public class InsertPlayerBaseAnimationsEntity extends PlayerBaseAnimationsEntity {

	@Builder
	public InsertPlayerBaseAnimationsEntity(Integer playerId, Integer playerPartId, Integer baseAnimationId, Integer color) {
		super(playerId, playerPartId, baseAnimationId, color);
	}

}
