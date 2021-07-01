package main.database.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import main.database.DbConnection;
import main.database.dto.PickableDto;

public class PickableDao {
	private static ArrayList<PickableDto> pickables;
	private static Map<Integer, Map<Integer, Set<Integer>>> pickableInstances; // floor, <sceneryId, <tileIds>>
	
	public static void setupCaches() {
		cachePickables();
		cachePickableInstances();
	}
	
	private static void cachePickables() {
		final String query = "select scenery_id, item_id, respawn_ticks, scenery_attributes from pickable";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
			ResultSet rs = ps.executeQuery()
		) {
			pickables = new ArrayList<>();
			
			while (rs.next())
				pickables.add(new PickableDto(rs.getInt("scenery_id"), rs.getInt("item_id"), rs.getInt("respawn_ticks"), rs.getInt("scenery_attributes")));

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	private static void cachePickableInstances() {
		pickableInstances = new HashMap<>();
		
		// run through every rock's scenery_id and pull out all the instance tiles
		for (PickableDto dto : pickables) {
			final String query = "select floor, tile_id from room_scenery where scenery_id = ?";
			
			try (
				Connection connection = DbConnection.get();
				PreparedStatement ps = connection.prepareStatement(query);
			) {
				ps.setInt(1, dto.getSceneryId());
				
				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						pickableInstances.putIfAbsent(rs.getInt("floor"), new HashMap<>());
						pickableInstances.get(rs.getInt("floor")).putIfAbsent(dto.getSceneryId(), new HashSet<>());
						pickableInstances.get(rs.getInt("floor")).get(dto.getSceneryId()).add(rs.getInt("tile_id"));
					}
				}	
				
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static PickableDto getPickableByTileId(int floor, int tileId) {
		if (!pickableInstances.containsKey(floor))
			return null;
		
		for (Map.Entry<Integer, Set<Integer>> instances : pickableInstances.get(floor).entrySet()) {
			if (instances.getValue().contains(tileId)) {
				for (PickableDto dto : pickables) {
					if (dto.getSceneryId() == instances.getKey())
						return dto;
				}
			}
		}
		return null;
	}
	
	public static PickableDto getPickableBySceneryId(int sceneryId) {
		return pickables.stream().filter(e -> e.getSceneryId() == sceneryId).findFirst().orElse(null);
	}
	
	public static Map<Integer, Set<Integer>> getPickablesByFloor(int floor) {
		if (!pickableInstances.containsKey(floor))
			return new HashMap<>();
		return pickableInstances.get(floor);
	}
}
