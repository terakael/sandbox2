package main.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import lombok.Getter;

public class SceneryDao {
	private SceneryDao() {};
	
	@Getter private static List<SceneryDto> allScenery;
	private static HashMap<Integer, List<SceneryDto>> allSceneryByRoom;
	private static HashMap<Integer, HashMap<Integer, HashSet<Integer>>> sceneryInstancesByRoom; // roomId, <sceneryId, tileids>
	
	public static void setupCaches() {
		allScenery = loadAllScenery();
		allSceneryByRoom = new HashMap<>();
		sceneryInstancesByRoom = new HashMap<>();
		for (int roomId : GroundTextureDao.getDistinctRoomIds()) {
			allSceneryByRoom.put(roomId, loadAllSceneryByRoom(roomId));
			sceneryInstancesByRoom.put(roomId, loadSceneryInstancesByRoom(roomId));
		}
	}
	
	@Getter private static HashMap<Integer, HashMap<Integer, Integer>> impassableTileIds = new HashMap<>();// roomId, <tile_id, impassable_type>
	
	private static List<SceneryDto> loadAllScenery() {
		final String query = 
				"select id, name, sprite_map_id, x, y, w, h, anchor_x, anchor_y, framecount, framerate, leftclick_option, other_options, attributes from scenery " +
				" where id in (select distinct scenery_id from room_scenery where id != 49)"; // 49 is impassable tile, don't send this to client (used in world builder tool and pathfinding)
		
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
					dto.setSpriteMapId(rs.getInt("sprite_map_id"));
					dto.setX(rs.getInt("x"));
					dto.setY(rs.getInt("y"));
					dto.setW(rs.getInt("w"));
					dto.setH(rs.getInt("h"));
					dto.setAnchorX(rs.getFloat("anchor_x"));
					dto.setAnchorY(rs.getFloat("anchor_y"));
					dto.setFramecount(rs.getInt("framecount"));
					dto.setFramerate(rs.getInt("framerate"));
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

	private static List<SceneryDto> loadAllSceneryByRoom(int roomId) {
		// TODO query is wrong
		final String query = 
				"select id, name, sprite_map_id, x, y, w, h, anchor_x, anchor_y, framecount, framerate, leftclick_option, other_options, attributes from scenery " +
				" where id in (select distinct scenery_id from room_scenery where room_id=? and id != 49)"; // 49 is impassable tile, don't send this to client (used in world builder tool and pathfinding)
		
		List<SceneryDto> sceneryList = new ArrayList<>();
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, roomId);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					SceneryDto dto = new SceneryDto();
					dto.setId(rs.getInt("id"));
					dto.setName(rs.getString("name"));
					dto.setSpriteMapId(rs.getInt("sprite_map_id"));
					dto.setX(rs.getInt("x"));
					dto.setY(rs.getInt("y"));
					dto.setW(rs.getInt("w"));
					dto.setH(rs.getInt("h"));
					dto.setAnchorX(rs.getFloat("anchor_x"));
					dto.setAnchorY(rs.getFloat("anchor_y"));
					dto.setFramecount(rs.getInt("framecount"));
					dto.setFramerate(rs.getInt("framerate"));
					dto.setLeftclickOption(rs.getInt("leftclick_option"));
					dto.setOtherOptions(rs.getInt("other_options"));
//					dto.setInstances(getInstanceListByRoomIdAndSceneryId(roomId, rs.getInt("id")));
					dto.setAttributes(rs.getInt("attributes"));
					sceneryList.add(dto);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return sceneryList;
	}
	
	public static HashMap<Integer, HashSet<Integer>> loadSceneryInstancesByRoom(int roomId) {
		HashMap<Integer, HashSet<Integer>> sceneryInstances = new HashMap<>();
		for (SceneryDto dto : allSceneryByRoom.get(roomId)) {
			sceneryInstances.put(dto.getId(), getInstanceListByRoomIdAndSceneryId(roomId, dto.getId()));
		}
		return sceneryInstances;
	}
	
	public static List<SceneryDto> getAllSceneryByRoom(int roomId) {
		if (!allSceneryByRoom.containsKey(roomId))
			return new ArrayList<>();
		return allSceneryByRoom.get(roomId);
	}
	
	public static HashMap<Integer, HashSet<Integer>> getAllSceneryInstancesByRoom(int roomId) {
		if (!sceneryInstancesByRoom.containsKey(roomId))
			return new HashMap<>();
		return sceneryInstancesByRoom.get(roomId);
	}
	
	public static HashSet<Integer> getInstanceListByRoomIdAndSceneryId(int roomId, int sceneryId) {
		String query = "select tile_id from room_scenery where room_id=? and scenery_id=?";
		
		HashSet<Integer> instanceList = new HashSet<>();
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, roomId);
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
	
	public static void addRoomScenery(int roomId, int sceneryId, int tileId) {
		String query = "insert into room_scenery (room_id, scenery_id, tile_id) values (?, ?, ?)";
		try (
				Connection connection = DbConnection.get();
				PreparedStatement ps = connection.prepareStatement(query)
			) {
				ps.setInt(1, roomId);
				ps.setInt(2, sceneryId);
				ps.setInt(3, tileId);
				ps.executeUpdate();
			} catch (SQLException e) {
				e.printStackTrace();
			}
	}
	
	public static HashMap<Integer, Integer> getImpassableTileIdsByRoomId(int roomId) {
		String query = "select tile_id, impassable from room_scenery ";
		query += " inner join scenery on room_scenery.scenery_id=scenery.id and impassable > 0";
		query += " where room_id=?";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, roomId);
			try (ResultSet rs = ps.executeQuery()) {
				impassableTileIds.put(roomId, new HashMap<>());
				while (rs.next()) {
					impassableTileIds.get(roomId).put(rs.getInt("tile_id"), rs.getInt("impassable"));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return impassableTileIds.get(roomId);
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
	
	public static int getSceneryIdByTileId(int roomId, int tileId) {
		if (!allSceneryByRoom.containsKey(roomId))
			return -1;
		
		if (!sceneryInstancesByRoom.containsKey(roomId))
			return -1;
		
		for (SceneryDto dto : allSceneryByRoom.get(roomId)) {
			if (sceneryInstancesByRoom.get(roomId).containsKey(dto.getId())) {
				if (sceneryInstancesByRoom.get(roomId).get(dto.getId()).contains(tileId))
					return dto.getId();
			}
		}
		return -1;
	}
	
	public static int getImpassableTypeByTileId(int roomId, int tileId) {
		if (!impassableTileIds.containsKey(roomId))
			return 0;
		
		if (impassableTileIds.get(roomId).containsKey(tileId))
			return impassableTileIds.get(roomId).get(tileId);
		
		return 0;
	}
}
