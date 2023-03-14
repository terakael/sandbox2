package database.entity.delete;

import database.entity.HousePetsEntity;
import database.entity.annotations.Operation;
import lombok.Builder;

@Operation("delete")
public class DeleteHousePetsEntity extends HousePetsEntity {

	@Builder
	public DeleteHousePetsEntity(Integer houseId, Integer petId, Integer floor) {
		super(houseId, petId, floor);
	}

}
