package types;

import lombok.Getter;

public enum ImpassableTypes {
	TOP(1),
	LEFT(2),
	RIGHT(4),
	BOTTOM(8),
	
	// if the low flags are set, it means it can be ranged/maged over (fence, wall with window etc)
	TOP_IS_LOW(16),
	LEFT_IS_LOW(32),
	RIGHT_IS_LOW(64),
	BOTTOM_IS_LOW(128);
	
	@Getter private int value;
	ImpassableTypes(int value) {
		this.value = value;
	}
	
	public static ImpassableTypes withValue(final int val) {
		for (ImpassableTypes type : ImpassableTypes.values()) {
			if (type.getValue() == val)
				return type;
		}
		return null;
	}
	
	public static boolean isImpassable(ImpassableTypes destination, int impassable) {
		return (impassable & destination.value) == destination.value;
	}
}
