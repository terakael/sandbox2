package main.types;

import lombok.Getter;

public enum ItemAttributes {
	STACKABLE(1),
	TRADEABLE(2),
	UNIQUE(4);// player can only have one of this item (i.e. will not see on ground if it's in inv/bank/already one on ground)
	
	@Getter private int value;
	ItemAttributes(int value) {
		this.value = value;
	}
	
	public static ItemAttributes withValue(final int val) {
		for (ItemAttributes attribute : ItemAttributes.values()) {
			if (attribute.getValue() == val)
				return attribute;
		}
		return null;
	}
}
