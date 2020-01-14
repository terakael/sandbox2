package main.types;

import lombok.Getter;

public enum DamageTypes {
	STANDARD(0),
	POISON(1),
	MAGIC(2);
	
	@Getter private int value;
	DamageTypes(int value) {
		this.value = value;
	}
	
	public static DamageTypes withValue(final int val) {
		for (DamageTypes type : DamageTypes.values()) {
			if (type.getValue() == val)
				return type;
		}
		return null;
	}
}
