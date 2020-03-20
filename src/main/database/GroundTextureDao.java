package main.database;

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
import main.processing.PathFinder;

public class GroundTextureDao {
	private static int SEGMENT_SIZE = 5; // 5x5 segments (must be divisible by PathFinder.LENGTH i.e. 250)
	private static HashMap<Integer, ArrayList<GroundTextureDto>> dtoMapByRoom = new HashMap<>();
	private static Map<Integer, Map<Integer, List<Integer>>> segmentsByRoom = new HashMap<>(); // <roomId, <segmentId, <groundTextureId>>>
	@Getter private static List<GroundTextureDto> dtosWithoutInstances = new ArrayList<>();
	@Getter private static HashSet<Integer> distinctRoomIds = new HashSet<>();
	
	public static void cacheSegments() {
		// note the ordering, this is important so the segment list is in the correct order
		final String query = "select room_id, tile_id, ground_texture_id from room_ground_textures order by room_id, tile_id";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query)
		) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					final int roomId = rs.getInt("room_id");
					final int tileId = rs.getInt("tile_id");
					final int groundTextureId = rs.getInt("ground_texture_id");
					
					if (!segmentsByRoom.containsKey(roomId))
						segmentsByRoom.put(roomId, new HashMap<>());
					
					int segmentId = getSegmentByTileId(tileId);
					if (!segmentsByRoom.get(roomId).containsKey(segmentId))
						segmentsByRoom.get(roomId).put(segmentId, new ArrayList<>());
					
					segmentsByRoom.get(roomId).get(segmentId).add(groundTextureId);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static Integer getSegmentByTileId(int tileId) {
		int segmentX = (tileId % PathFinder.LENGTH) / SEGMENT_SIZE; // convert the tileX of 0-250 into the segmentX of 0-50
		int segmentY = (tileId / PathFinder.LENGTH) / SEGMENT_SIZE;
		
		// segment id is just like a tileId; an element in an array, so we calculate the array element the same way (y*len + x)
		return (segmentY * (PathFinder.LENGTH / SEGMENT_SIZE)) + segmentX;
	}
	
	public static Set<Integer> getSegmentGroupByTileId(int tileId) {
		Set<Integer> segmentGroup = new HashSet<>();
		
		int centreSegmentId = getSegmentByTileId(tileId);
		
		// 3x3 segment grid, each of which containing a 5x5 subgrid of tileIds
		
		for (int y = -2; y <= 2; ++y) {
			for (int x = -2; x <= 2; ++x) {
				Integer neighbour = neighbourIsValid(centreSegmentId, x, y);
				if (neighbour != null)
					segmentGroup.add(neighbour);
			}
		}
		
//		Integer neighbour = neighbourIsValid(centreSegmentId, -2, -2);
//		if (neighbour != null)
//			segmentGroup.add(neighbour);
//		
//		int topLeftSegmentId = centreSegmentId - segmentGridLength - 1;
//		if (topLeftSegmentId >= 0 && topLeftSegmentId % segmentGridLength != segmentGridLength - 1)
//			segmentGroup.add(topLeftSegmentId);
//		
//		int topSegmentId = centreSegmentId - segmentGridLength;
//		if (topSegmentId >= 0)
//			segmentGroup.add(topSegmentId);
//
//		int topRightSegmentId = centreSegmentId - segmentGridLength + 1;
//		if (topRightSegmentId % segmentGridLength != 0)
//			segmentGroup.add(topRightSegmentId);
//		
//		// left
//		int leftSegmentId = centreSegmentId - 1;
//		if (leftSegmentId % segmentGridLength != segmentGridLength - 1)
//			segmentGroup.add(leftSegmentId);
//		
//		int rightSegmentId = centreSegmentId + 1;
//		if (rightSegmentId % segmentGridLength != 0)
//			segmentGroup.add(rightSegmentId);
//		
//		int bottomLeftSegmentId = centreSegmentId + segmentGridLength - 1;
//		if (bottomLeftSegmentId < (segmentGridLength * segmentGridLength) && bottomLeftSegmentId % segmentGridLength != segmentGridLength - 1)
//			segmentGroup.add(bottomLeftSegmentId);
//		
//		int bottomSegmentId = centreSegmentId + segmentGridLength;
//		if (bottomSegmentId < (segmentGridLength * segmentGridLength))
//			segmentGroup.add(bottomSegmentId);
//		
//		int bottomRightSegmentId = centreSegmentId + segmentGridLength + 1;
//		if (bottomRightSegmentId < (segmentGridLength * segmentGridLength) && bottomRightSegmentId % segmentGridLength != 0)
//			segmentGroup.add(bottomRightSegmentId);
//		
		return segmentGroup;
	}
	
	private static Integer neighbourIsValid(int centre, int xOffset, int yOffset) {
		final int segmentGridLength = PathFinder.LENGTH / SEGMENT_SIZE;
		
		int neighbourId = centre + (yOffset * segmentGridLength) + xOffset;
		
		// vertical check
		if (neighbourId < 0 || neighbourId >= segmentGridLength * segmentGridLength)
			return null;
		
		// horizontal check
		if ((centre % segmentGridLength) + xOffset < 0 || (centre % segmentGridLength) + xOffset >= segmentGridLength)
			return null;
		
		return neighbourId;
	}
	
	public static List<Integer> getGroundTextureIdsByRoomIdSegmentId(int roomId, int segmentId) {
		if (!segmentsByRoom.containsKey(roomId))
			return null;
		
		if (!segmentsByRoom.get(roomId).containsKey(segmentId))
			return null;
		
		return segmentsByRoom.get(roomId).get(segmentId);
	}
	
	public static void cacheDistinctRoomIds() {
		final String query = "select distinct room_id from room_ground_textures";
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query)
		) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					distinctRoomIds.add(rs.getInt("room_id"));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static void cacheTextures() {
		final String query = "select id, sprite_map_id, x, y from ground_textures ";
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query)
		) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					dtosWithoutInstances.add(new GroundTextureDto(rs.getInt("id"), 0, rs.getInt("sprite_map_id"), rs.getInt("x"), rs.getInt("y"), null));
					
					HashMap<Integer, HashSet<Integer>> instancesByRoom = getInstancesByGroundTextureId(rs.getInt("id"));
					for (Map.Entry<Integer, HashSet<Integer>> entry : instancesByRoom.entrySet()) {
						if (!dtoMapByRoom.containsKey(entry.getKey()))
							dtoMapByRoom.put(entry.getKey(), new ArrayList<>());
						dtoMapByRoom.get(entry.getKey())
									.add(new GroundTextureDto(
										rs.getInt("id"), 
										entry.getKey(), 
										rs.getInt("sprite_map_id"), 
										rs.getInt("x"), 
										rs.getInt("y"), 
										entry.getValue()));
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static HashMap<Integer, HashSet<Integer>> getInstancesByGroundTextureId(int textureId) {
		final String query = "select room_id, tile_id from room_ground_textures where ground_texture_id=?";
		HashMap<Integer, HashSet<Integer>> instances = new HashMap<>();
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query)
		) {
			ps.setInt(1, textureId);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					if (!instances.containsKey(rs.getInt("room_id")))
						instances.put(rs.getInt("room_id"), new HashSet<>());
					instances.get(rs.getInt("room_id")).add(rs.getInt("tile_id"));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return instances;
	}
	
	public static HashSet<Integer> getStandardTileSetSpriteMaps() {
		HashSet<Integer> spriteMapIds = new HashSet<>();
		final String query = "select distinct sprite_map_id from ground_textures where id > 100";// 101 and above are all standard tileset sprite maps
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query)
		) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					spriteMapIds.add(rs.getInt("sprite_map_id"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return spriteMapIds;
	}
	
	public static ArrayList<GroundTextureDto> getAllGroundTexturesByRoom(int roomId) {
		return dtoMapByRoom.get(roomId);
	}
}
