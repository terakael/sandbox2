package database.entity;

import database.entity.annotations.Column;
import database.entity.annotations.Id;
import database.entity.annotations.Table;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Table("house_pets")
public class HousePetsEntity extends UpdateableEntity {
	@Id
	@Column("house_id")
	protected final Integer houseId;
	
	@Id
	@Column("pet_id")
	protected final Integer petId;
	
	@Column("floor")
	protected final Integer floor;
}
