package database.entity.insert;

import lombok.Builder;
import database.entity.ArtisanTaskEntity;
import database.entity.annotations.Operation;

@Operation("insert")
public class InsertArtisanTaskEntity extends ArtisanTaskEntity {
	@Builder
	public InsertArtisanTaskEntity(Integer playerId, Integer itemId, Integer progress1, Integer progress2,
			Integer progress3, Integer progress4, Integer progress5, Integer progress6, Integer progress7,
			Integer progress8, Integer progress9) {
		super(playerId, itemId, progress1, progress2, progress3, progress4, progress5, progress6, progress7, progress8, progress9);
	}

}
