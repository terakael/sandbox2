package processing.scenery;

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
		
		sceneryMap.put(107, new Door());
		sceneryMap.put(108, new Door());
		sceneryMap.put(109, new Door());
		sceneryMap.put(110, new Door());
		sceneryMap.put(111, new Door());
		sceneryMap.put(112, new Door());
		sceneryMap.put(113, new Door());
		sceneryMap.put(114, new Door());
		sceneryMap.put(115, new Door());
		sceneryMap.put(116, new Door());
		
		sceneryMap.put(120, new Anvil());
		sceneryMap.put(121, new Sawmill());
		
		// constructable fires
		sceneryMap.put(122, new Fire()); // regular
		sceneryMap.put(123, new Fire());
		sceneryMap.put(124, new Fire());
		sceneryMap.put(125, new Fire());
		sceneryMap.put(126, new Fire());
		sceneryMap.put(127, new Fire()); // magic
		
		sceneryMap.put(128, new Sawmill());
		sceneryMap.put(144, new Furnace());
		sceneryMap.put(151, new Workbench());
	}
	public static Scenery getScenery(int sceneryId) {
		if (sceneryMap.containsKey(sceneryId))
			return sceneryMap.get(sceneryId);
		return null;
	}
}
