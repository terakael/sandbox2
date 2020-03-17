package main.types;

import lombok.Getter;

public enum Stats {
	STRENGTH(1), 
	ACCURACY(2), 
	DEFENCE(3), 
	AGILITY(4), 
	HITPOINTS(5), 
	MINING(6),
	SMITHING(7),
	MAGIC(8),
	HERBLORE(9),
	FISHING(10),
	COOKING(11);
	
	@Getter private int value;
	Stats(int value) {
		this.value = value;
	}
	
	public static Stats withValue(final int val) {
		for (Stats stat : Stats.values()) {
			if (stat.getValue() == val)
				return stat;
		}
		return null;
	}
}
