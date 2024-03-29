package database.entity.insert;

import database.entity.ArtisanTaskEntity;
import database.entity.annotations.Operation;
import lombok.Builder;

@Operation("insert")
public class InsertArtisanTaskEntity extends ArtisanTaskEntity {

	@Builder
	public InsertArtisanTaskEntity(Integer playerId, Integer assignedMasterId, Integer itemId, Integer assignedAmount,
			Integer handedInAmount, Integer totalTasks, Integer totalPoints) {
		super(playerId, assignedMasterId, itemId, assignedAmount, handedInAmount, totalTasks, totalPoints);
	}



}
