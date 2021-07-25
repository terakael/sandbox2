package database.entity.delete;

import database.entity.ArtisanTaskBreakdownEntity;
import database.entity.annotations.Operation;
import lombok.Builder;

@Operation("delete")
public class DeleteArtisanTaskBreakdownEntity extends ArtisanTaskBreakdownEntity {

	@Builder
	public DeleteArtisanTaskBreakdownEntity(Integer playerId, Integer itemId, Integer amount) {
		super(playerId, itemId, amount);
	}

}
