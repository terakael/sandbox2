package database.dao;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import database.DbConnection;
import database.dto.SceneryDto;
import database.entity.delete.DeleteRoomSceneryEntity;
import database.entity.insert.InsertRoomSceneryEntity;
import lombok.Getter;
import processing.PathFinder;
import processing.managers.ConstructableManager;
import processing.managers.DatabaseUpdater;
import processing.managers.UndeadArmyManager;
import types.SceneryAttributes;
import types.SceneryContextOptions;

public class SceneryDao {
	private SceneryDao() {
	};

	private static Map<Integer, SceneryDto> allScenery = new LinkedHashMap<>();
	private static HashMap<Integer, Set<SceneryDto>> allSceneryByFloor;

	@Getter
	private static Map<Integer, Map<Integer, Set<Integer>>> sceneryInstancesByFloor; // floor, <sceneryId, tileids>

	public static void setupCaches() {
		loadAllScenery();
		allSceneryByFloor = new HashMap<>();
		sceneryInstancesByFloor = new HashMap<>();
		for (int floor : GroundTextureDao.getDistinctFloors()) {
			allSceneryByFloor.put(floor, loadAllSceneryByFloor(floor));
			sceneryInstancesByFloor.put(floor, loadSceneryInstancesByFloor(floor));
		}
	}

	private static void loadAllScenery() {
		final String query = "select id, name, sprite_frame_id, leftclick_option, other_options, impassable, attributes, lightsource_radius from scenery "
				+
				" where attributes != 2 ";

		DbConnection.load(query, rs -> {
			final SceneryDto dto = new SceneryDto();
			dto.setId(rs.getInt("id"));
			dto.setName(rs.getString("name"));
			dto.setSpriteFrameId(rs.getInt("sprite_frame_id"));
			dto.setLeftclickOption(rs.getInt("leftclick_option"));
			dto.setOtherOptions(rs.getInt("other_options"));
			dto.setImpassable(rs.getInt("impassable"));
			dto.setAttributes(rs.getInt("attributes"));
			dto.setLightsourceRadius(rs.getInt("lightsource_radius"));
			allScenery.put(rs.getInt("id"), dto);
		});
	}

	private static Set<SceneryDto> loadAllSceneryByFloor(int floor) {
		final String query = "select " +
				"     id " +
				"    from " +
				"     scenery " +
				"    where " +
				"     id in ( " +
				"     select " +
				"      distinct scenery_id " +
				"     from " +
				"      ( " +
				"      SELECT " +
				"       scenery_id " +
				"      FROM " +
				"       custom_room_scenery crs " +
				"      JOIN custom_area ca ON " +
				"       crs.custom_area_id = ca.id " +
				"      where " +
				"       ca.floor + crs.offset_floor = ? " +
				"     union all " +
				"      select " +
				"       distinct scenery_id " +
				"      from " +
				"       room_scenery " +
				"      where " +
				"       floor = ? " +
				"    ) t " +
				"    ) " +
				"    and attributes != 2";

		Set<SceneryDto> sceneryList = new HashSet<>();

		DbConnection.load(query, rs -> {
			final SceneryDto dto = allScenery.get(rs.getInt("id"));
			if (dto != null)
				sceneryList.add(dto);
		}, floor, floor);

		return sceneryList;
	}

	private static Map<Integer, Set<Integer>> loadSceneryInstancesByFloor(int floor) {
		Map<Integer, Set<Integer>> sceneryInstances = new HashMap<>();
		for (SceneryDto dto : allSceneryByFloor.get(floor)) {
			sceneryInstances.put(dto.getId(), getInstanceListByFloorAndSceneryId(floor, dto.getId()));
		}
		return sceneryInstances;
	}

	public static Map<Integer, Set<Integer>> getAllSceneryInstancesByFloor(int floor) {
		if (!sceneryInstancesByFloor.containsKey(floor))
			return new HashMap<>();
		return sceneryInstancesByFloor.get(floor);
	}

	public static Set<Integer> getInstanceListByFloorAndSceneryId(int floor, int sceneryId) {
		String query = "select tile_id from room_scenery where floor=? and scenery_id=?";

		HashSet<Integer> instanceList = new HashSet<>();
		DbConnection.load(query,
				rs -> {
					if (!GroundTextureDao.hasCustomTile(floor, rs.getInt("tile_id")))
						instanceList.add(rs.getInt("tile_id"));
				}, floor, sceneryId);

		String customSceneryQuery = "SELECT " +
				" ca.tile_id + (? * crs.offset_y) + crs.offset_x AS tile_id " +
				" FROM custom_room_scenery crs " +
				" JOIN custom_area ca ON crs.custom_area_id = ca.id " +
				" where ca.floor + crs.offset_floor=? and scenery_id=?";

		DbConnection.load(customSceneryQuery,
				rs -> instanceList.add(rs.getInt("tile_id")), PathFinder.LENGTH, floor, sceneryId);

		return instanceList;
	}

	public static HashMap<Integer, String> getExamineMap() {
		HashMap<Integer, String> examineMap = new HashMap<>();
		DbConnection.load("select id, examine from scenery",
				rs -> examineMap.put(rs.getInt("id"), rs.getString("examine")));
		return examineMap;
	}

	public static Set<SceneryDto> getAllScenery() {
		return new LinkedHashSet<>(allScenery.values());
	}

	public static int getSceneryIdByTileId(int floor, int tileId) {
		if (!allSceneryByFloor.containsKey(floor))
			return -1;

		if (!sceneryInstancesByFloor.containsKey(floor))
			return -1;

		for (SceneryDto dto : allSceneryByFloor.get(floor)) {
			if (sceneryInstancesByFloor.get(floor).containsKey(dto.getId())) {
				if (sceneryInstancesByFloor.get(floor).get(dto.getId()).contains(tileId))
					return dto.getId();
			}
		}

		// fallback by checking the constructables
		int constructableId = ConstructableManager.getConstructableIdByTileId(floor, tileId);
		if (constructableId != -1)
			return constructableId;

		// fall back even more by checking if this is one of the undead army trees
		int undeadArmyTreeId = UndeadArmyManager.getSceneryIdByTileId(floor, tileId);
		if (undeadArmyTreeId != -1)
			return undeadArmyTreeId;

		return -1;
	}

	public static int getImpassableTypeByFloorAndTileId(int floor, int tileId) {
		if (!sceneryInstancesByFloor.containsKey(floor))
			return 0;

		for (var entry : sceneryInstancesByFloor.get(floor).entrySet()) {
			if (entry.getValue().contains(tileId))
				return allScenery.get(entry.getKey()).getImpassable();
		}

		return 0;
	}

	public static int getIdByName(String name) {
		return allScenery.values().stream()
				.filter(e -> name.equals(e.getName()))
				.findFirst()
				.map(SceneryDto::getId)
				.orElse(-1);
	}

	public static String getNameById(int id) {
		if (allScenery.containsKey(id))
			return allScenery.get(id).getName();
		return "";
	}

	public static boolean sceneryContainsAttribute(int sceneryId, SceneryAttributes attribute) {
		final SceneryDto dto = allScenery.get(sceneryId);
		if (dto == null)
			return false;

		return (dto.getAttributes() & attribute.getValue()) > 0;
	}

	public static boolean sceneryContainsContextOption(int sceneryId, SceneryContextOptions option) {
		final SceneryDto dto = allScenery.get(sceneryId);
		if (dto == null)
			return false;

		return ((dto.getLeftclickOption() & option.getValue()) |
				(dto.getOtherOptions() & option.getValue())) > 0;
	}

	public static void upsertRoomScenery(int floor, int tileId, int sceneryId) {
		final SceneryDto dto = allScenery.get(sceneryId);
		if (dto == null) {
			// invalid scenery
			return;
		}

		// if some scenery already exists at this location then get rid of it
		deleteRoomScenery(floor, tileId);

		allSceneryByFloor.putIfAbsent(floor, new HashSet<>());
		allSceneryByFloor.get(floor).add(dto);

		sceneryInstancesByFloor.putIfAbsent(floor, new HashMap<>());
		sceneryInstancesByFloor.get(floor).putIfAbsent(sceneryId, new HashSet<>());
		sceneryInstancesByFloor.get(floor).get(sceneryId).add(tileId);

		DatabaseUpdater.enqueue(new InsertRoomSceneryEntity(floor, tileId, sceneryId));
	}

	public static boolean deleteRoomScenery(int floor, int tileId) {
		if (!sceneryInstancesByFloor.containsKey(floor))
			return false;

		final Set<Integer> containedScenery = sceneryInstancesByFloor.get(floor).values().stream()
				.filter(e -> e.contains(tileId))
				.findFirst()
				.orElse(null);

		if (containedScenery != null) {
			if (containedScenery.remove(tileId)) {
				DatabaseUpdater.enqueue(new DeleteRoomSceneryEntity(floor, tileId, null));
				return true;
			}
		}
		return false;
	}

	public static SceneryDto getSceneryById(int sceneryId) {
		return allScenery.get(sceneryId);
	}

	public static void replaceTileIdsForSceneries(
			Collection<Integer> targetSceneryIds,
			BiFunction<Integer, Integer, Integer> tileIdMapper) { // Changed to BiFunction

		if (sceneryInstancesByFloor == null || targetSceneryIds == null || targetSceneryIds.isEmpty()
				|| tileIdMapper == null) {
			System.err.println("Invalid input: One or more arguments are null or empty.");
			return; // Or throw an IllegalArgumentException
		}

		Set<Integer> targetSet = new HashSet<>(targetSceneryIds);

		// Iterate through each floor
		for (Map.Entry<Integer, Map<Integer, Set<Integer>>> floorEntry : sceneryInstancesByFloor.entrySet()) {
			int currentFloor = floorEntry.getKey(); // Get the current floor number
			Map<Integer, Set<Integer>> sceneryOnFloor = floorEntry.getValue();

			if (sceneryOnFloor == null) {
				continue;
			}

			// Identify scenery IDs to process on this floor (avoids
			// ConcurrentModificationException)
			List<Integer> sceneryIdsToProcessOnFloor = new ArrayList<>();
			for (Integer sceneryId : sceneryOnFloor.keySet()) {
				if (targetSet.contains(sceneryId)) {
					sceneryIdsToProcessOnFloor.add(sceneryId);
				}
			}

			// Process the targeted scenery IDs for this floor
			for (Integer sceneryId : sceneryIdsToProcessOnFloor) {
				Set<Integer> originalTileIds = sceneryOnFloor.get(sceneryId);
				if (originalTileIds == null || originalTileIds.isEmpty()) {
					continue;
				}

				// Create a new set with the transformed tile IDs using the BiFunction
				// Stream approach:
				Set<Integer> newTileIds = originalTileIds.stream()
						// The lambda now captures 'currentFloor' and passes it to the mapper
						.map(oldTileId -> tileIdMapper.apply(currentFloor, oldTileId))
						.collect(Collectors.toSet());

				/*
				 * // Alternative loop approach:
				 * Set<Integer> newTileIds = new HashSet<>();
				 * for (int oldTileId : originalTileIds) {
				 * // Pass both floor and oldTileId to the mapper
				 * int newTileId = tileIdMapper.apply(currentFloor, oldTileId);
				 * newTileIds.add(newTileId);
				 * }
				 */

				// Replace the old set with the new set
				sceneryOnFloor.put(sceneryId, newTileIds);
			}
		}
	}

	public static Map<Integer, Map<Integer, Set<Integer>>> filterSceneryByIds(List<Integer> sceneryIdsToKeep) {

		// Use a Set for efficient lookups
		Set<Integer> keepSet = new HashSet<>(sceneryIdsToKeep);

		// Handle null/empty inputs
		if (sceneryInstancesByFloor == null || sceneryInstancesByFloor.isEmpty() || keepSet.isEmpty()) {
			return new HashMap<>();
		}

		return sceneryInstancesByFloor.entrySet().stream() // Stream<Map.Entry<Integer, Map<Integer, Set<Integer>>>>
				.map(floorEntry -> {
					// For each floor, filter its inner map (scenery map)
					Map<Integer, Set<Integer>> filteredInnerMap = floorEntry.getValue().entrySet().stream()
							.filter(sceneryEntry -> keepSet.contains(sceneryEntry.getKey())) // Keep only desired
																								// scenery IDs
							.collect(Collectors.toMap(
									Map.Entry::getKey, // Key is the sceneryId
									Map.Entry::getValue // Value is the Set<Integer> of tileIds
					));
					// Return a temporary pair (or Entry) holding the floor and its filtered inner
					// map
					// Using AbstractMap.SimpleEntry as a convenient Pair implementation
					return new AbstractMap.SimpleEntry<>(floorEntry.getKey(), filteredInnerMap);
				})
				.filter(entry -> !entry.getValue().isEmpty()) // IMPORTANT: Filter out floors where the inner map became
																// empty
				.collect(Collectors.toMap(
						Map.Entry::getKey, // Key is the floor number
						Map.Entry::getValue // Value is the filtered inner map for that floor
				));
	}

	public static void removeSceneryFromTileId(int floor, int tileId, int sceneryId) {
		if (!sceneryInstancesByFloor.containsKey(floor))
			return;

		if (!sceneryInstancesByFloor.get(floor).containsKey(sceneryId))
			return;

		sceneryInstancesByFloor.get(floor).get(sceneryId).remove(tileId);
	}

	public static void insertSceneryAtTileId(int floor, int tileId, int sceneryId) {
		sceneryInstancesByFloor.putIfAbsent(floor, new HashMap<>());
		sceneryInstancesByFloor.get(floor).putIfAbsent(sceneryId, new HashSet<>());
		sceneryInstancesByFloor.get(floor).get(sceneryId).add(tileId);
	}
}
