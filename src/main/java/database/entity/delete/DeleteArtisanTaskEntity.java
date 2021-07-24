package database.entity.delete;

import database.entity.ArtisanTaskEntity;
import database.entity.annotations.Operation;
import lombok.Builder;

@Operation("delete")
public class DeleteArtisanTaskEntity extends ArtisanTaskEntity {

	@Builder
	public DeleteArtisanTaskEntity(Integer playerId, Integer itemId, Integer amount) {
		super(playerId, itemId, amount);
	}

}
