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
	
	private static List<MineableDto> mineables = null;
	@Getter private static HashMap<Integer, HashMap<Integer, HashSet<Integer>>> mineableInstances = null;
	
	public static void setupCaches() {
		cacheMineables();
		cacheMineableInstances();
	}
	
	public static MineableDto getMineableDtoByTileId(int roomId, int tileId) {
		if (!mineableInstances.containsKey(roomId))
			return null;
		
		for (HashMap.Entry<Integer, HashSet<Integer>> instances : mineableInstances.get(roomId).entrySet()) {
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
			final String query = "select room_id, tile_id from room_scenery where scenery_id = ?";
			
			try (
				Connection connection = DbConnection.get();
				PreparedStatement ps = connection.prepareStatement(query);
			) {
				ps.setInt(1, dto.getSceneryId());
				
				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						if (!mineableInstances.containsKey(rs.getInt("room_id")))
							mineableInstances.put(rs.getInt("room_id"), new HashMap<>());
						
						if (!mineableInstances.get(rs.getInt("room_id")).containsKey(dto.getSceneryId()))
							mineableInstances.get(rs.getInt("room_id")).put(dto.getSceneryId(), new HashSet<>());
						
						mineableInstances.get(rs.getInt("room_id")).get(dto.getSceneryId()).add(rs.getInt("tile_id"));
					}
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static void cacheMineables() {
		final String query = "select scenery_id, level, exp, item_id, respawn_ticks from mineable";
		
		List<MineableDto> dtos = new ArrayList<>();
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
			ResultSet rs = ps.executeQuery()
		) {
			while (rs.next())
				dtos.add(new MineableDto(rs.getInt("scenery_id"), rs.getInt("level"), rs.getInt("exp"), rs.getInt("item_id"), rs.getInt("respawn_ticks")));

			mineables = dtos;
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static int getMineableExpByItemId(int itemId) {
		final String query = "select exp from mineable where item_id=?";
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, itemId);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next())
					return rs.getInt("exp");
			}	
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;
	}
}
