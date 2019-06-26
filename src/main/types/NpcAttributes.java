package main.types;

import lombok.Getter;

public enum NpcAttributes {
	AGGRESSIVE(1);
	
	@Getter private int value;
	NpcAttributes(int value) {
		this.value = value;
	}
	
	public static NpcAttributes withValue(final int val) {
		for (NpcAttributes attribute : NpcAttributes.values()) {
			if (attribute.getValue() == val)
				return attribute;
		}
		return null;
	}
}
