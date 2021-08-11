package database.entity.insert;

import database.entity.PlayerArtisanBlockedTaskEntity;
import database.entity.annotations.Operation;
import lombok.Builder;

@Operation("insert")
public class InsertPlayerArtisanBlockedTaskEntity extends PlayerArtisanBlockedTaskEntity {

	@Builder
	public InsertPlayerArtisanBlockedTaskEntity(Integer playerId, Integer item1, Integer item2, Integer item3, Integer item4, Integer item5) {
		super(playerId, item1, item2, item3, item4, item5);
	}
	
}
