package main.scenery;

import java.util.HashMap;
import java.util.Map;

public class SceneryManager {
	private static Map<Integer, Scenery> sceneryMap = new HashMap<>();
	static {
		sceneryMap.put(19, new Furnace());
		sceneryMap.put(20, new Fire());
	}
	public static Scenery getScenery(int sceneryId) {
		if (sceneryMap.containsKey(sceneryId))
			return sceneryMap.get(sceneryId);
		return null;
	}
}
