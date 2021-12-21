package types;

import lombok.Getter;

public enum SceneryAttributes {
	UNUSABLE(1),
	INVISIBLE(2),
	NOCTURNAL(4),
	DIURNAL(8);
	
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
