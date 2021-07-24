package database.entity.insert;

import lombok.Builder;
import database.entity.ArtisanTaskEntity;
import database.entity.annotations.Operation;

@Operation("insert")
public class InsertArtisanTaskEntity extends ArtisanTaskEntity {

	@Builder
	public InsertArtisanTaskEntity(Integer playerId, Integer itemId, Integer amount) {
		super(playerId, itemId, amount);
	}

}
