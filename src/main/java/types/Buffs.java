package types;

import lombok.Getter;

public enum Buffs {
	RESTORATION(1, 50),
	GOBLIN_STANK(2, 100);
	
	@Getter private int value;
	@Getter private int maxTicks;
	Buffs(int value, int maxTicks) {
		this.value = value;
		this.maxTicks = maxTicks;
	}
	
	public static Stats withValue(final int val) {
		for (Stats stat : Stats.values()) {
			if (stat.getValue() == val)
				return stat;
		}
		return null;
	}	
}
