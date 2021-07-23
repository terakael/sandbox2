package types;

import lombok.Getter;

public enum ShopTypes {
	GENERAL(1),
	SPECIALTY(2);

	@Getter private int value;
	ShopTypes(int value) {
		this.value = value;
	}
	
	public static ShopTypes withValue(final int val) {
		for (ShopTypes type : ShopTypes.values()) {
			if (type.getValue() == val)
				return type;
		}
		return null;
	}
}
