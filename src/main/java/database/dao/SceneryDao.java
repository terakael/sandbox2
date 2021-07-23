package database.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Getter;
import database.DbConnection;
import database.dto.SceneryDto;
import processing.managers.ConstructableManager;
import processing.managers.UndeadArmyManager;
import types.SceneryAttributes;

public class SceneryDao {
	private SceneryDao() {};
	
	@Getter private static List<SceneryDto> allScenery = new ArrayList<>();
	private static HashMap<Integer, List<SceneryDto>> allSceneryByFloor;
	private static Map<Integer, Map<Integer, Set<Integer>>> sceneryInstancesByFloor; // floor, <sceneryId, tileids>
	@Getter private static HashMap<Integer, HashMap<Integer, Integer>> impassableTileIds = new HashMap<>();// floor, <tile_id, impassable_type>
	
	public static void setupCaches() {
		loadAllScenery();
		allSceneryByFloor = new HashMap<>();
		sceneryInstancesByFloor = new HashMap<>();
		for (int floor : GroundTextureDao.getDistinctFloors()) {
			allSceneryByFloor.put(floor, loadAllSceneryByFloor(floor));
			sceneryInstancesByFloor.put(floor, loadSceneryInstancesByFloor(floor));
			
			cacheImpassableTileIdsByFloor(floor);
		}
	}
	
	private static void loadAllScenery() {
		final String query = 
				"select id, name, sprite_frame_id, leftclick_option, other_options, attributes, lightsource_radius from scenery " +
						" where attributes != 2 ";
		
		DbConnection.load(query, rs -> {
			final SceneryDto dto = new SceneryDto();
			dto.setId(rs.getInt("id"));
			dto.setName(rs.getString("name"));
			dto.setSpriteFrameId(rs.getInt("sprite_frame_id"));
			dto.setLeftclickOption(rs.getInt("leftclick_option"));
			dto.setOtherOptions(rs.getInt("other_options"));
			dto.setAttributes(rs.getInt("attributes"));
			dto.setLightsourceRadius(rs.getInt("lightsource_radius"));
			allScenery.add(dto);
		});
	}

	private static List<SceneryDto> loadAllSceneryByFloor(int floor) {
		final String query = 
				"select id, name, sprite_frame_id, leftclick_option, other_options, attributes, lightsource_radius from scenery " +
				" where id in (select distinct scenery_id from room_scenery where floor=?) and attributes != 2";
		
		List<SceneryDto> sceneryList = new ArrayList<>();
		
		DbConnection.load(query, rs -> {
			SceneryDto dto = new SceneryDto();
			dto.setId(rs.getInt("id"));
			dto.setName(rs.getString("name"));
			dto.setSpriteFrameId(rs.getInt("sprite_frame_id"));
			dto.setLeftclickOption(rs.getInt("leftclick_option"));
			dto.setOtherOptions(rs.getInt("other_options"));
			dto.setAttributes(rs.getInt("attributes"));
			dto.setLightsourceRadius(rs.getInt("lightsource_radius"));
			sceneryList.add(dto);
		}, floor);
		
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
				rs -> instanceList.add(rs.getInt("tile_id")), floor, sceneryId);
		
		return instanceList;
	}
	
	public static void cacheImpassableTileIdsByFloor(int floor) {
		String query = "select tile_id, impassable from room_scenery ";
		query += " inner join scenery on room_scenery.scenery_id=scenery.id and impassable > 0";
		query += " where floor=?";
		
		impassableTileIds.put(floor, new HashMap<>());
		DbConnection.load(query, rs -> {
			impassableTileIds.get(floor).put(rs.getInt("tile_id"), rs.getInt("impassable"));
		}, floor);
	}
	
	public static HashMap<Integer, String> getExamineMap() {
		HashMap<Integer, String> examineMap = new HashMap<>();
		DbConnection.load("select id, examine from scenery", 
				rs -> examineMap.put(rs.getInt("id"), rs.getString("examine")));
		return examineMap;
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
	
	public static int getImpassableTypeByFloor(int floor, int tileId) {
		if (!impassableTileIds.containsKey(floor))
			return 0;
		
		if (impassableTileIds.get(floor).containsKey(tileId))
			return impassableTileIds.get(floor).get(tileId);
		
		return 0;
	}
	
	public static HashMap<Integer, Integer> getImpassableTileIdsByFloor(int floor) {
		return impassableTileIds.get(floor);
	}
	
	public static int getIdByName(String name) {
		return allScenery.stream()
				.filter(e -> name.equals(e.getName()))
				.findFirst()
				.map(SceneryDto::getId)
				.orElse(-1);
	}
	
	public static String getNameById(int id) {
		return allScenery.stream()
				.filter(e -> e.getId() == id)
				.findFirst()
				.map(SceneryDto::getName)
				.orElse("");
	}
	
	public static boolean sceneryContainsAttribute(int sceneryId, SceneryAttributes attribute) {
		SceneryDto dto = allScenery.stream().filter(scenery -> scenery.getId() == sceneryId).findFirst().orElse(null);
		if (dto == null)
			return false;
		
		return (dto.getAttributes() & attribute.getValue()) > 0;
	}
}
