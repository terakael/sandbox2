package main.types;

import lombok.Getter;

public enum SceneryAttributes {
	UNUSABLE(1);
	
	@Getter private int value;
	SceneryAttributes(int value) {
		this.value = value;
	}
	
	public static SceneryAttributes withValue(final int val) {
		for (SceneryAttributes attribute : SceneryAttributes.values()) {
			if (attribute.getValue() == val)
				return attribute;
		}
		return null;
	}
}
