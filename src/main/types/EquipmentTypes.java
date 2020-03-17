package main.types;

import lombok.Getter;

public enum EquipmentTypes {
	HELMET(1),
	BODY(2),
	LEGS(3),
	SHIELD(4),
	SWORD(5),
	HAMMER(6),
	DAGGERS(7),
	RING(8),
	CAPE(9),
	NECKLACE(10);
	
	@Getter private int value;
	EquipmentTypes(int value) {
		this.value = value;
	}
	
	public static EquipmentTypes withValue(final int val) {
		for (EquipmentTypes type : EquipmentTypes.values()) {
			if (type.getValue() == val)
				return type;
		}
		return null;
	}
}
