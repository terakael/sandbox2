package database.entity.update;

import lombok.Builder;
import database.entity.TybaltsTaskEntity;
import database.entity.annotations.Operation;

@Operation("update")
public class UpdateTybaltsTaskEntity extends TybaltsTaskEntity {
	@Builder
	public UpdateTybaltsTaskEntity(Integer playerId, Integer taskId, Integer progress1, Integer progress2, Integer progress3, Integer progress4) {
		super(playerId, taskId, progress1, progress2, progress3, progress4);
	}
}
