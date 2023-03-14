package database.entity.update;

import database.entity.HousingConstructableStorageEntity;
import database.entity.annotations.Operation;
import lombok.Builder;

@Operation("update")
public class UpdateHousingConstructableStorageEntity extends HousingConstructableStorageEntity {

	@Builder
	public UpdateHousingConstructableStorageEntity(Integer floor, Integer tileId, Integer playerId, Integer constructableId, Integer slot, Integer itemId, Integer count, Integer charges) {
		super(floor, tileId, playerId, constructableId, slot, itemId, count, charges);
	}

}
