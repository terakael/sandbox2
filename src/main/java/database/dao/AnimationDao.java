package database.dao;

import java.util.HashMap;
import java.util.Map;

import database.DbConnection;
import database.dto.AnimationDto;

public class AnimationDao {
	private static Map<Integer, AnimationDto> animations = new HashMap<>();
	
	public static void setupCaches() {
		cacheAllAnimations();
	}
	
	private static void cacheAllAnimations() {
		final String query = "select id, sprite_map_id, up_id, down_id, left_id, right_id, attack_left_id, attack_right_id from animations";
		DbConnection.load(query, rs -> {
			animations.put(rs.getInt("id"), new AnimationDto(
					rs.getInt("id"), 
					rs.getInt("sprite_map_id"), 
					rs.getInt("up_id"),
					rs.getInt("down_id"),
					rs.getInt("left_id"),
					rs.getInt("right_id"),
					rs.getInt("attack_left_id"),
					rs.getInt("attack_right_id")));
		});
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
