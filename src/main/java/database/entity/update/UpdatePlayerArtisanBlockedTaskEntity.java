package database.entity.update;

import database.entity.PlayerArtisanBlockedTaskEntity;
import database.entity.annotations.Operation;
import lombok.Builder;

@Operation("update")
public class UpdatePlayerArtisanBlockedTaskEntity extends PlayerArtisanBlockedTaskEntity {

	@Builder
	public UpdatePlayerArtisanBlockedTaskEntity(Integer playerId, Integer item1, Integer item2, Integer item3, Integer item4, Integer item5) {
		super(playerId, item1, item2, item3, item4, item5);
	}

}
