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

public class MineableDao {
	private MineableDao() {}
	
	@Getter private static List<MineableDto> mineables = null;
	@Getter private static HashSet<Integer> mineableTiles = null;
	@Getter private static HashMap<Integer, HashSet<Integer>> mineableInstances = null;
	
	public static void setupCaches() {
		cacheMineables();
		cacheMineableTiles();
		cacheMineableInstances();
	}
	
	public static MineableDto getMineableDtoByTileId(int tileId) {
		for (HashMap.Entry<Integer, HashSet<Integer>> instances : mineableInstances.entrySet()) {
			if (instances.getValue().contains(tileId)) {
				for (MineableDto dto : mineables) {
					if (dto.getSceneryId() == instances.getKey())
						return dto;
				}
			}
		}
		return null;
	}
	
	private static void cacheMineableInstances() {
		mineableInstances = new HashMap<>();
		
		// run through every rock's scenery_id and pull out all the instance tiles
		for (MineableDto dto : mineables) {
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
				
				mineableInstances.put(dto.getSceneryId(), instances);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static void cacheMineableTiles() {
		final String query = "select tile_id from room_scenery where scenery_id in (select scenery_id from mineable)";
		
		mineableTiles = new HashSet<>();
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
			ResultSet rs = ps.executeQuery()
		) {
			while (rs.next())
				mineableTiles.add(rs.getInt("tile_id"));
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	private static void cacheMineables() {
		final String query = "select scenery_id, level, exp, item_id from mineable";
		
		List<MineableDto> dtos = new ArrayList<>();
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
			ResultSet rs = ps.executeQuery()
		) {
			while (rs.next())
				dtos.add(new MineableDto(rs.getInt("scenery_id"), rs.getInt("level"), rs.getInt("exp"), rs.getInt("item_id")));

			mineables = dtos;
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
