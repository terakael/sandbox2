package types;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EquipmentTypes {
	HELMET_FULL(1),
	BODY(2),
	LEGS(3),
	SHIELD(4),
	SWORD(5),
	HAMMER(6),
	DAGGERS(7),
	RING(8),
	CAPE(9),
	NECKLACE(10),
	HELMET_MED(11),
	HAT(12),
	WAND(13);
	
	private final int value;
	
	public static EquipmentTypes withValue(final int val) {
		for (EquipmentTypes type : EquipmentTypes.values()) {
			if (type.getValue() == val)
				return type;
		}
		return null;
	}
}
