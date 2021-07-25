package database.entity.insert;

import database.entity.ArtisanTaskBreakdownEntity;
import database.entity.annotations.Operation;
import lombok.Builder;

@Operation("insert")
public class InsertArtisanTaskBreakdownEntity extends ArtisanTaskBreakdownEntity {

	@Builder
	public InsertArtisanTaskBreakdownEntity(Integer playerId, Integer itemId, Integer amount) {
		super(playerId, itemId, amount);
	}

}
