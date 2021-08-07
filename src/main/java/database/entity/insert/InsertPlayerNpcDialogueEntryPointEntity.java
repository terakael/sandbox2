package database.entity.insert;

import database.entity.PlayerNpcDialogueEntryPointEntity;
import database.entity.annotations.Operation;
import lombok.Builder;

@Operation("insert")
public class InsertPlayerNpcDialogueEntryPointEntity extends PlayerNpcDialogueEntryPointEntity {

	@Builder
	public InsertPlayerNpcDialogueEntryPointEntity(Integer playerId, Integer npcId, Integer pointId) {
		super(playerId, npcId, pointId);
	}

}
