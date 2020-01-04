package main.types;

import lombok.Getter;

public enum ImpassableTypes {
	TOP(1),
	LEFT(2),
	RIGHT(4),
	BOTTOM(8);
	
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
