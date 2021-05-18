package main.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

public class CastableDao {
	private static HashMap<Integer, CastableDto> castables; // itemId, dto
	private static HashMap<Integer, Integer> spriteMapIdByCastable;
	
	public static void setupCaches() {
		cacheCastables();
		cacheSpriteMaps();
	}
	
	private static void cacheCastables() {
		final String query = "select item_id, level, exp, max_hit, sprite_frame_id from castable";
		
		castables = new HashMap<>();
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					castables.put(rs.getInt("item_id"), new CastableDto(rs.getInt("item_id"), rs.getInt("level"), rs.getInt("exp"), rs.getInt("max_hit"), rs.getInt("sprite_frame_id")));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	private static void cacheSpriteMaps() {
		final String query = "select castable.item_id, sprite_frames.sprite_map_id from castable " + 
				"inner join sprite_frames on sprite_frames.id = castable.sprite_frame_id";
		
		spriteMapIdByCastable = new HashMap<>();
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					spriteMapIdByCastable.put(rs.getInt("item_id"), rs.getInt("sprite_map_id"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static boolean isCastable(int itemId) {
		return castables.containsKey(itemId);
	}
	
	public static CastableDto getCastableByItemId(int itemId) {
		if (isCastable(itemId))
			return castables.get(itemId);
		return null;
	}
	
	public static int getSpriteMapIdByItemId(int itemId) {
		return spriteMapIdByCastable.get(itemId);
	}
}
