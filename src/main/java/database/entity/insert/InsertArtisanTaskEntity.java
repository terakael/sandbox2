package database.entity.insert;

import database.entity.ArtisanTaskEntity;
import database.entity.annotations.Operation;
import lombok.Builder;

@Operation("insert")
public class InsertArtisanTaskEntity extends ArtisanTaskEntity {

	@Builder
	public InsertArtisanTaskEntity(Integer playerId, Integer itemId, Integer assignedAmount, Integer handedInAmount) {
		super(playerId, itemId, assignedAmount, handedInAmount);
	}

}
