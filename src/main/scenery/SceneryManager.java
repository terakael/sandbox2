package main.scenery;

import java.util.HashMap;
import java.util.Map;

public class SceneryManager {
	private static Map<Integer, Scenery> sceneryMap = new HashMap<>();
	static {
		sceneryMap.put(19, new Furnace());
		sceneryMap.put(20, new Fire());
		sceneryMap.put(21, new DoubtObelisk());
		sceneryMap.put(22, new GuiltObelisk());
		sceneryMap.put(23, new ShameObelisk());
		sceneryMap.put(24, new FearObelisk());
		sceneryMap.put(25, new GriefObelisk());
		sceneryMap.put(26, new BloodObelisk());
		sceneryMap.put(27, new DeathObelisk());
		sceneryMap.put(106, new Altar());
	}
	public static Scenery getScenery(int sceneryId) {
		if (sceneryMap.containsKey(sceneryId))
			return sceneryMap.get(sceneryId);
		return null;
	}
}
