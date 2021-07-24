package database.entity.update;

import database.entity.ArtisanTaskItemEntity;
import database.entity.annotations.Operation;
import lombok.Builder;

@Operation("update")
public class UpdateArtisanTaskItemEntity extends ArtisanTaskItemEntity {

	@Builder
	public UpdateArtisanTaskItemEntity(Integer playerId, Integer itemId, Integer assignedAmount, Integer handedInAmount) {
		super(playerId, itemId, assignedAmount, handedInAmount);
	}

}
