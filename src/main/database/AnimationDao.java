package main.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class AnimationDao {
	private static Map<Integer, AnimationDto> animations = null;
	
	public static void setupCaches() {
		cacheAllAnimations();
	}
	
	private static void cacheAllAnimations() {
		animations = new HashMap<>();
		
		String query = "select id, sprite_map_id, up_id, down_id, left_id, right_id, attack_left_id, attack_right_id from animations";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					animations.put(rs.getInt("id"), new AnimationDto(
															rs.getInt("id"), 
															rs.getInt("sprite_map_id"), 
															rs.getInt("up_id"),
															rs.getInt("down_id"),
															rs.getInt("left_id"),
															rs.getInt("right_id"),
															rs.getInt("attack_left_id"),
															rs.getInt("attack_right_id")));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static int getSpriteMapIdByAnimationId(int animationId) {
		if (!animations.containsKey(animationId))
			return -1;
		return animations.get(animationId).getSpriteMapId();
	}
	
	public static AnimationDto getAnimationDtoById(int animationId) {
		if (!animations.containsKey(animationId))
			return null;
		return animations.get(animationId);
	}
}
