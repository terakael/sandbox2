package database.entity.delete;

import database.entity.HousingConstructableStorageEntity;
import database.entity.annotations.Operation;
import lombok.Builder;

@Operation("delete")
public class DeleteHousingConstructableStorageEntity extends HousingConstructableStorageEntity {

	@Builder
	public DeleteHousingConstructableStorageEntity(Integer floor, Integer tileId, Integer playerId, Integer constructableId, Integer slot, Integer itemId, Integer count, Integer charges) {
		super(floor, tileId, playerId, constructableId, slot, itemId, count, charges);
	}

}
