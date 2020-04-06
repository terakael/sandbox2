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

public class GroundTextureDao {
	@Getter private static List<GroundTextureDto> groundTextures = new ArrayList<>();
	@Getter private static HashSet<Integer> distinctFloors = new HashSet<>();
	
	private static Map<Integer, Map<Integer, Set<Integer>>> tileIdsByGroundTextureId = new HashMap<>(); // floor, <groundTextureId, tileId>
	
	public static void cacheTileIdsByGroundTextureId() {
		final String query = "select floor, ground_texture_id, tile_id from room_ground_textures";
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query)
		) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					final int floor = rs.getInt("floor");
					final int groundTextureId = rs.getInt("ground_texture_id");
					final int tileId = rs.getInt("tile_id");
					
					if (!tileIdsByGroundTextureId.containsKey(floor))
						tileIdsByGroundTextureId.put(floor, new HashMap<>());
					
					if (!tileIdsByGroundTextureId.get(floor).containsKey(groundTextureId))
						tileIdsByGroundTextureId.get(floor).put(groundTextureId, new HashSet<>());
					
					tileIdsByGroundTextureId.get(floor).get(groundTextureId).add(tileId);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static Integer getGroundTextureIdByTileId(int floor, int tileId) {
		if (!tileIdsByGroundTextureId.containsKey(floor))
			return 0;
		
		for (Map.Entry<Integer, Set<Integer>> entry : tileIdsByGroundTextureId.get(floor).entrySet()) {
			if (entry.getValue().contains(tileId))
				return entry.getKey();
		}
		
		return 0;
	}
	
	public static void cacheDistinctFloors() {
		final String query = "select distinct floor from room_ground_textures";
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query)
		) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					distinctFloors.add(rs.getInt("floor"));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static void cacheTextures() {
		final String query = "select id, sprite_map_id, x, y, walkable from ground_textures ";
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query)
		) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					groundTextures.add(new GroundTextureDto(rs.getInt("id"), rs.getInt("sprite_map_id"), rs.getInt("x"), rs.getInt("y"), rs.getBoolean("walkable")));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static HashMap<Integer, HashSet<Integer>> getInstancesByGroundTextureId(int textureId) {
		final String query = "select floor, tile_id from room_ground_textures where ground_texture_id=?";
		HashMap<Integer, HashSet<Integer>> instances = new HashMap<>();
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query)
		) {
			ps.setInt(1, textureId);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					if (!instances.containsKey(rs.getInt("floor")))
						instances.put(rs.getInt("floor"), new HashSet<>());
					instances.get(rs.getInt("floor")).add(rs.getInt("tile_id"));
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
	
	public static Set<Integer> getAllWalkableTileIdsByFloor(int floor) {
		Set<Integer> allTileIdsByFloor = new HashSet<>();
		
		final String query = "select tile_id from room_ground_textures where floor=? and ground_texture_id in "
								+ "(select id from ground_textures where walkable=1)";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query)
		) {
			ps.setInt(1, floor);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					allTileIdsByFloor.add(rs.getInt("tile_id"));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return allTileIdsByFloor;
	}
}
