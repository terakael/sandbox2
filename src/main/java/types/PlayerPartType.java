package types;

import lombok.Getter;

public enum PlayerPartType {
	HEAD(1),
	TORSO(2),
	LEGS(3),
	ONHAND(4),
	OFFHAND(5),
	RING(6),
	CAPE(7),
	NECKLACE(8),
	
	HAIR(9),
	SHIRT(10),
	PANTS(11),
	SHOES(12),
	BEARD(13);
	
	@Getter private int value;
	PlayerPartType(int value) {
		this.value = value;
	}
	
	public static PlayerPartType withValue(final int val) {
		for (PlayerPartType type : PlayerPartType.values()) {
			if (type.getValue() == val)
				return type;
		}
		return null;
	}
}
