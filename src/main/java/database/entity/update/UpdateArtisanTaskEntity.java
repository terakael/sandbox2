package database.entity.update;

import database.entity.ArtisanTaskEntity;
import database.entity.annotations.Operation;
import lombok.Builder;

@Operation("update")
public class UpdateArtisanTaskEntity extends ArtisanTaskEntity {

	@Builder
	public UpdateArtisanTaskEntity(Integer playerId, Integer itemId, Integer assignedAmount, Integer handedInAmount) {
		super(playerId, itemId, assignedAmount, handedInAmount);
	}

}
