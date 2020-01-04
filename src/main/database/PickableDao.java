package main.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class PickableDao {
	private static ArrayList<PickableDto> pickables;
	private static HashMap<Integer, HashSet<Integer>> pickableInstances;
	
	public static void setupCaches() {
		cachePickables();
		cachePickableInstances();
	}
	
	private static void cachePickables() {
		final String query = "select scenery_id, item_id, respawn_ticks from pickable";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
			ResultSet rs = ps.executeQuery()
		) {
			pickables = new ArrayList<>();
			
			while (rs.next())
				pickables.add(new PickableDto(rs.getInt("scenery_id"), rs.getInt("item_id"), rs.getInt("respawn_ticks")));

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	private static void cachePickableInstances() {
		pickableInstances = new HashMap<>();
		
		// run through every rock's scenery_id and pull out all the instance tiles
		for (PickableDto dto : pickables) {
			final String query = "select tile_id from room_scenery where scenery_id = ?";
			
			try (
				Connection connection = DbConnection.get();
				PreparedStatement ps = connection.prepareStatement(query);
			) {
				ps.setInt(1, dto.getSceneryId());
				
				HashSet<Integer> instances = new HashSet<>();
				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						instances.add(rs.getInt("tile_id"));
					}
				}	
				
				pickableInstances.put(dto.getSceneryId(), instances);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static PickableDto getPickableByTileId(int tileId) {
		for (HashMap.Entry<Integer, HashSet<Integer>> instances : pickableInstances.entrySet()) {
			if (instances.getValue().contains(tileId)) {
				for (PickableDto dto : pickables) {
					if (dto.getSceneryId() == instances.getKey())
						return dto;
				}
			}
		}
		return null;
	}
}
