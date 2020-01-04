package main.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class GroundTextureDao {
	private static HashMap<Integer, ArrayList<GroundTextureDto>> dtoMapByRoom = new HashMap<>();
	
	public static void cacheTextures() {
		final String query = "select id, sprite_map_id, x, y from ground_textures ";
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query)
		) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
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
