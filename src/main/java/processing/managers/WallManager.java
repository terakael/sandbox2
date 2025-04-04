package processing.managers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import database.DbConnection;
import database.dao.SceneryDao;
import processing.PathFinder;

public class WallManager {
	private static Map<Integer, Map<Integer, Integer>> wallInstances; // floor, <tileId, sceneryId>

	public static void setupCaches() {
		wallInstances = new HashMap<>();
		String query = "SELECT ca.floor + cwi.offset_floor AS floor, " +
				" ca.tile_id + (" + PathFinder.LENGTH + " * cwi.offset_y) + cwi.offset_x AS tile_id, scenery_id " +
				" FROM custom_wall_instances cwi " +
				" JOIN custom_area ca ON cwi.custom_area_id = ca.id" +
				" UNION ALL " +
				" select floor, tile_id, scenery_id from wall_instances";

		DbConnection.load(query, rs -> {
			wallInstances.putIfAbsent(rs.getInt("floor"), new HashMap<>());
			wallInstances.get(rs.getInt("floor")).put(rs.getInt("tile_id"), rs.getInt("scenery_id"));
		});
	}

	public static int getWallSceneryIdByFloorAndTileId(int floor, int tileId) {
		if (wallInstances.containsKey(floor) && wallInstances.get(floor).containsKey(tileId))
			return wallInstances.get(floor).get(tileId);
		return -1;
	}

	public static int getImpassableTypeByFloorAndTileId(int floor, int tileId) {
		if (!wallInstances.containsKey(floor))
			return 0;

		if (!wallInstances.get(floor).containsKey(tileId))
			return 0;

		// wall_instance scenery_id is FK so sceneryId should be guaranteed to exist in
		// the scenery map
		return SceneryDao.getSceneryById(wallInstances.get(floor).get(tileId)).getImpassable();
	}

	public static Map<Integer, Integer> getImpassableTypeByFloor(int floor) {
		if (!wallInstances.containsKey(floor))
			return new HashMap<>();

		return wallInstances.get(floor).entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey,
						e -> SceneryDao.getSceneryById(e.getValue()).getImpassable()));
	}

	public static Set<Integer> getWallTileIdsByFloor(int floor) {
		if (!wallInstances.containsKey(floor))
			return new HashSet<>();
		return wallInstances.get(floor).keySet();
	}
}
