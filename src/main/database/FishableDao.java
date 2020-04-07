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

public class FishableDao {
	private static List<FishableDto> fishables = null;
	@Getter private static HashMap<Integer, HashSet<Integer>> fishableInstances = null; 
	
	public static void setupCaches() {
		cacheFishables();
		cacheFishableInstances();
	}
	
	public static FishableDto getFishableDtoByTileId(int tileId) {
		for (HashMap.Entry<Integer, HashSet<Integer>> instances : fishableInstances.entrySet()) {
			if (instances.getValue().contains(tileId)) {
				for (FishableDto dto : fishables) {
					if (dto.getSceneryId() == instances.getKey())
						return dto;
				}
			}
		}
		return null;
	}
	
	private static void cacheFishables() {
		final String query = "select scenery_id, level, exp, item_id, respawn_ticks from fishable";
		
		List<FishableDto> dtos = new ArrayList<>();
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
			ResultSet rs = ps.executeQuery()
		) {
			while (rs.next())
				dtos.add(new FishableDto(rs.getInt("scenery_id"), rs.getInt("level"), rs.getInt("exp"), rs.getInt("item_id"), rs.getInt("respawn_ticks")));

			fishables = dtos;
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	private static void cacheFishableInstances() {
		fishableInstances = new HashMap<>();
		
		// run through every rock's scenery_id and pull out all the instance tiles
		for (FishableDto dto : fishables) {
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
				
				fishableInstances.put(dto.getSceneryId(), instances);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
}
