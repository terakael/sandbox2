package database.entity.update;

import database.entity.PlayerNpcDialogueEntryPointEntity;
import database.entity.annotations.Operation;
import lombok.Builder;

@Operation("update")
public class UpdatePlayerNpcDialogueEntryPointEntity extends PlayerNpcDialogueEntryPointEntity {

	@Builder
	public UpdatePlayerNpcDialogueEntryPointEntity(Integer playerId, Integer npcId, Integer pointId) {
		super(playerId, npcId, pointId);
	}

}
