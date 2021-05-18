package main.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import main.types.PlayerPartType;

public class PlayerAnimationDao {	
	private static Map<Integer, Map<PlayerPartType, PlayerAnimationDto>> playerBaseAnimations;
	private static Map<Integer, Set<Integer>> spriteMapIdsByPlayerId = new HashMap<>(); // playerid, set<sprite_map_id>
	
	private PlayerAnimationDao() {}
	
	public static void setupCaches() {
		cacheBasePlayerAnimations();
		cachePlayerAnimationSpriteMapIds();
	}
	
	public static Map<PlayerPartType, PlayerAnimationDto> loadAnimationsByPlayerId(int playerId) {		
		Map<PlayerPartType, PlayerAnimationDto> animationMap = new HashMap<>();
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
								new PlayerAnimationDto(
									rs.getInt("up_id"), 
									rs.getInt("down_id"),
									rs.getInt("left_id"),
									rs.getInt("right_id"),
									rs.getInt("attack_left_id"),
									rs.getInt("attack_right_id"),
									null));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static void cachePlayerAnimationSpriteMapIds() {
		final String query = "select player_id, sprite_frames.sprite_map_id from player_animations " + 
				"inner join sprite_frames on sprite_frames.id = up_id";// just pull the up_id, the down/left/right etc all come from the same sprite map
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					if (!spriteMapIdsByPlayerId.containsKey(rs.getInt("player_id")))
						spriteMapIdsByPlayerId.put(rs.getInt("player_id"), new HashSet<>());
					spriteMapIdsByPlayerId.get(rs.getInt("player_id")).add(rs.getInt("sprite_map_id"));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static Set<Integer> getSpriteMapIdsByPlayerIds(Set<Integer> playerIds) {
		Set<Integer> spriteMapIds = new HashSet<>();
		for (Integer playerId : playerIds) {
			if (spriteMapIdsByPlayerId.containsKey(playerId))
				spriteMapIds.addAll(spriteMapIdsByPlayerId.get(playerId));
		}
		return spriteMapIds;
	}
}
