package types;

import lombok.Getter;

public enum SceneryContextOptions {
	OPEN(2),
	CLIMB(4),
	FISH(8),
	MINE(16),
	CATCH(32),
	ENTER(128),
	PICK(256),
	PRAY_AT(512),
	CHOP(1024),
	REPAIR(2048),
	DRINK_FROM(4096),
	LOOT(8192),
	CLIMB_THROUGH(16384),
	BUILD(32678);
	
	@Getter private int value;
	SceneryContextOptions(int value) {
		this.value = value;
	}
	
	public static SceneryContextOptions withValue(final int val) {
		for (SceneryContextOptions attribute : SceneryContextOptions.values()) {
			if (attribute.getValue() == val)
				return attribute;
		}
		return null;
	}
}
