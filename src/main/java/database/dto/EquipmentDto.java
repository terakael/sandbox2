package database.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import types.EquipmentTypes;

@Setter @Getter @AllArgsConstructor
public class EquipmentDto {
	private int itemId;
	private int partId;
	private int requirement;
	private EquipmentTypes type;
	private PlayerAnimationDto animations;
	private EquipmentBonusDto bonuses;
}
