package database.entity.update;

import lombok.Builder;
import database.entity.ArtisanTaskEntity;
import database.entity.annotations.Operation;

@Operation("update")
public class UpdateArtisanTaskEntity extends ArtisanTaskEntity {

	@Builder
	public UpdateArtisanTaskEntity(Integer playerId, Integer itemId, Integer amount) {
		super(playerId, itemId, amount);
	}

}
