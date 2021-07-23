package database.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Getter;
import database.DbConnection;
import database.dto.GroundTextureDto;

public class GroundTextureDao {
	@Getter private static List<GroundTextureDto> groundTextures = new ArrayList<>();
	@Getter private static HashSet<Integer> distinctFloors = new HashSet<>();
	
	private static Map<Integer, Map<Integer, Set<Integer>>> tileIdsByGroundTextureId = new HashMap<>(); // floor, <groundTextureId, tileId>
	
	public static void cacheTileIdsByGroundTextureId() {
		final String query = "select floor, ground_texture_id, tile_id from room_ground_textures";
		DbConnection.load(query, rs -> {
			final int floor = rs.getInt("floor");
			final int groundTextureId = rs.getInt("ground_texture_id");
			final int tileId = rs.getInt("tile_id");
			
			tileIdsByGroundTextureId.putIfAbsent(floor, new HashMap<>());
			tileIdsByGroundTextureId.get(floor).putIfAbsent(groundTextureId, new HashSet<>());
			tileIdsByGroundTextureId.get(floor).get(groundTextureId).add(tileId);
		});
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
		DbConnection.load("select distinct floor from room_ground_textures", 
				rs -> distinctFloors.add(rs.getInt("floor")));
	}
	
	public static void cacheTextures() {
		DbConnection.load("select id, sprite_map_id, x, y, walkable from ground_textures", 
				rs -> groundTextures.add(new GroundTextureDto(rs.getInt("id"), rs.getInt("sprite_map_id"), rs.getInt("x"), rs.getInt("y"), rs.getBoolean("walkable"))));
	}
	
	public static Set<Integer> getAllWalkableTileIdsByFloor(int floor) {
		Set<Integer> allTileIdsByFloor = new HashSet<>();
		
		final String query = "select tile_id from room_ground_textures where floor=? and ground_texture_id in "
								+ "(select id from ground_textures where walkable=1)";
		
		DbConnection.load(query, 
				rs -> allTileIdsByFloor.add(rs.getInt("tile_id")), 
				floor);
		
		return allTileIdsByFloor;
	}
}
