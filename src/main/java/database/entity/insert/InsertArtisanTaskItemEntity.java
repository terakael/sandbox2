package database.entity.insert;

import database.entity.ArtisanTaskItemEntity;
import database.entity.annotations.Operation;
import lombok.Builder;

@Operation("insert")
public class InsertArtisanTaskItemEntity extends ArtisanTaskItemEntity {

	@Builder
	public InsertArtisanTaskItemEntity(Integer playerId, Integer itemId, Integer assignedAmount, Integer handedInAmount) {
		super(playerId, itemId, assignedAmount, handedInAmount);
	}

}
