package database.entity.delete;

import database.entity.HousingConstructableEntity;
import database.entity.annotations.Operation;
import lombok.Builder;

@Operation("delete")
public class DeleteHousingConstructableEntity extends HousingConstructableEntity {

	@Builder
	public DeleteHousingConstructableEntity(Integer floor, Integer tileId, Integer constructableId) {
		super(floor, tileId, constructableId);
	}

}
