package main.database.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Getter;
import main.database.DbConnection;
import main.database.dto.SceneryDto;
import main.processing.ConstructableManager;

public class SceneryDao {
	private SceneryDao() {};
	
	@Getter private static List<SceneryDto> allScenery; // passed to player on page load
	private static HashMap<Integer, List<SceneryDto>> allSceneryByFloor;
	private static Map<Integer, Map<Integer, Set<Integer>>> sceneryInstancesByFloor; // floor, <sceneryId, tileids>
	@Getter private static HashMap<Integer, HashMap<Integer, Integer>> impassableTileIds = new HashMap<>();// floor, <tile_id, impassable_type>
	
	public static void setupCaches() {
		allScenery = loadAllScenery();
		allSceneryByFloor = new HashMap<>();
		sceneryInstancesByFloor = new HashMap<>();
		for (int floor : GroundTextureDao.getDistinctFloors()) {
			allSceneryByFloor.put(floor, loadAllSceneryByFloor(floor));
			sceneryInstancesByFloor.put(floor, loadSceneryInstancesByFloor(floor));
			
			cacheImpassableTileIdsByFloor(floor);
		}
	}
	
	private static List<SceneryDto> loadAllScenery() {
		final String query = 
				"select id, name, sprite_frame_id, leftclick_option, other_options, attributes from scenery " +
						" where attributes != 2 ";
		List<SceneryDto> sceneryList = new ArrayList<>();
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					SceneryDto dto = new SceneryDto();
					dto.setId(rs.getInt("id"));
					dto.setName(rs.getString("name"));
					dto.setSpriteFrameId(rs.getInt("sprite_frame_id"));
					dto.setLeftclickOption(rs.getInt("leftclick_option"));
					dto.setOtherOptions(rs.getInt("other_options"));
					dto.setAttributes(rs.getInt("attributes"));
					sceneryList.add(dto);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return sceneryList;
	}

	private static List<SceneryDto> loadAllSceneryByFloor(int floor) {
		final String query = 
				"select id, name, sprite_frame_id, leftclick_option, other_options, attributes from scenery " +
				" where id in (select distinct scenery_id from room_scenery where floor=?) and attributes != 2";
		
		List<SceneryDto> sceneryList = new ArrayList<>();
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, floor);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					SceneryDto dto = new SceneryDto();
					dto.setId(rs.getInt("id"));
					dto.setName(rs.getString("name"));
					dto.setSpriteFrameId(rs.getInt("sprite_frame_id"));
					dto.setLeftclickOption(rs.getInt("leftclick_option"));
					dto.setOtherOptions(rs.getInt("other_options"));
					dto.setAttributes(rs.getInt("attributes"));
					sceneryList.add(dto);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
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
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, floor);
			ps.setInt(2, sceneryId);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					instanceList.add(rs.getInt("tile_id"));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return instanceList;
	}
	
	public static void cacheImpassableTileIdsByFloor(int floor) {
		String query = "select tile_id, impassable from room_scenery ";
		query += " inner join scenery on room_scenery.scenery_id=scenery.id and impassable > 0";
		query += " where floor=?";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, floor);
			try (ResultSet rs = ps.executeQuery()) {
				impassableTileIds.put(floor, new HashMap<>());
				while (rs.next()) {
					impassableTileIds.get(floor).put(rs.getInt("tile_id"), rs.getInt("impassable"));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static HashMap<Integer, String> getExamineMap() {
		String query = "select id, examine from scenery";
		
		HashMap<Integer, String> examineMap = new HashMap<>();
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					examineMap.put(rs.getInt("id"), rs.getString("examine"));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
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
		return ConstructableManager.getConstructableIdByTileId(floor, tileId);
	}
	
	public static int getImpassableTypeByFloor(int floor, int tileId) {
		if (!impassableTileIds.containsKey(floor))
			return 0;
		
		if (impassableTileIds.get(floor).containsKey(tileId))
			return impassableTileIds.get(floor).get(tileId);
		
		return 0;
	}
	
	public static HashMap<Integer, Integer> getImpassableTileIdsByFloor(int floor) {
		if (!impassableTileIds.containsKey(floor))
			return null;
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
}
