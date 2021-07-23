package types;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DuelRules {
	no_retreat(1),
	no_magic(2),
	no_prayer(4),
	no_boosted_stats(8),
	no_poison(16),
	dangerous(32);
	
	private final int value;
	
	private static Map<Integer, String> map;
	
	static {
		map = new HashMap<>();
		for (DuelRules rule : values()) {
			map.put(rule.getValue(), rule.name().replace("_", " "));
		}
	}
	
	
	public static boolean isValidRule(int rule) {
		final DuelRules[] values = values();
		for (DuelRules value : values) {
			if (value.getValue() == rule)
				return true;
		}
		return false;
	}
	
	public static Map<Integer, String> asMap() {
		return map;
	}
}
