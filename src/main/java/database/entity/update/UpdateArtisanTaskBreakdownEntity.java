package database.entity.update;

import database.entity.ArtisanTaskBreakdownEntity;
import database.entity.annotations.Operation;
import lombok.Builder;

@Operation("update")
public class UpdateArtisanTaskBreakdownEntity extends ArtisanTaskBreakdownEntity {

	@Builder
	public UpdateArtisanTaskBreakdownEntity(Integer playerId, Integer itemId, Integer amount) {
		super(playerId, itemId, amount);
	}

}
