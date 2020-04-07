package main.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import main.types.PlayerPartType;

public class AnimationDao {	
	private static Map<Integer, Map<PlayerPartType, AnimationDto>> playerBaseAnimations;
	
	private AnimationDao() {}
	
	public static Map<PlayerPartType, AnimationDto> loadAnimationsByPlayerId(int playerId) {		
		Map<PlayerPartType, AnimationDto> animationMap = new HashMap<>();
		if (!playerBaseAnimations.containsKey(playerId))
			return animationMap;
		
		animationMap.put(PlayerPartType.HEAD, playerBaseAnimations.get(playerId).get(PlayerPartType.HEAD));
		animationMap.put(PlayerPartType.TORSO, playerBaseAnimations.get(playerId).get(PlayerPartType.TORSO));
		animationMap.put(PlayerPartType.LEGS, playerBaseAnimations.get(playerId).get(PlayerPartType.LEGS));
		return animationMap;
	}
	
	public static void cacheBasePlayerAnimations() {
		playerBaseAnimations = new HashMap<>();
		
		final String query = "select player_id, player_part_id, up_id, down_id, left_id, right_id, attack_left_id, attack_right_id from player_animations";
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					final int playerId = rs.getInt("player_id");
					if (!playerBaseAnimations.containsKey(playerId))
						playerBaseAnimations.put(playerId, new HashMap<>());
					
						playerBaseAnimations.get(playerId)
							.put(PlayerPartType.withValue(rs.getInt("player_part_id")), 
								new AnimationDto(
									rs.getInt("up_id"), 
									rs.getInt("down_id"),
									rs.getInt("left_id"),
									rs.getInt("right_id"),
									rs.getInt("attack_left_id"),
									rs.getInt("attack_right_id")));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
