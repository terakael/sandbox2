package database.entity.update;

import database.entity.ArtisanTaskEntity;
import database.entity.annotations.Operation;
import lombok.Builder;

@Operation("update")
public class UpdateArtisanTaskEntity extends ArtisanTaskEntity {

	@Builder
	public UpdateArtisanTaskEntity(Integer playerId, Integer assignedMasterId, Integer itemId, Integer assignedAmount,
			Integer handedInAmount, Integer totalTasks, Integer totalPoints) {
		super(playerId, assignedMasterId, itemId, assignedAmount, handedInAmount, totalTasks, totalPoints);
		// TODO Auto-generated constructor stub
	}

}
