package main.types;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum Prayers {
	THICK_SKIN(1), // implemented
	CALM_MIND(2), // implemented
	BURST_OF_STRENGTH(3), // implemented
	RAPID_RESTORE(4), // implemented
	RAPID_HEAL(5), // implemented
	PROTECT_SLOT(6), // implemented
	STONE_SKIN(7), // implemented
	FOCUSSED_MIND(8), // implemented
	SUPERIOR_STRENGTH(9), // implemented
	SLOW_BURN(10), // implemented
	FAITH_HEALING(11), // implemented
	STEALTH(12),
	SMITE(13), // implemented
	STEEL_SKIN(14), // implemented
	ZEN_MIND(15), // implemented
	ULTIMATE_STRENGTH(16), // implemented
	ULTRASMITE(17), // implemented
	PROTECT_SLOT_LVL_2(18),
	RUNELESS_MAGIC(19),
	RUNELESS_MAGIC_LVL_2(20),
	RUNELESS_MAGIC_LVL_3(21);
	
	@Getter int value;
	public static Prayers withValue(final int val) {
		for (Prayers type : Prayers.values()) {
			if (type.getValue() == val)
				return type;
		}
		return null;
	}
}
