package main.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import main.types.AnimationType;
import main.types.PlayerPartType;

public class AnimationDao {
	
	private AnimationDao() {}
	
	private static int getAnimationIdByPlayerId(int playerId, AnimationType type, PlayerPartType playerPart) {
		final String query = "select " + type.name() + "_id from player_animations where player_id=? and player_part_id=?";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, playerId);
			ps.setInt(2, playerPart.getValue());
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return rs.getInt(type.name() + "_id");
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return -1;
	}
	
	public static AnimationDto loadAnimationsByPlayerIdPartId(int playerId, PlayerPartType playerPart) {
		return new AnimationDto(
			AnimationDao.getAnimationIdByPlayerId(playerId, AnimationType.up, playerPart),
			AnimationDao.getAnimationIdByPlayerId(playerId, AnimationType.down, playerPart),
			AnimationDao.getAnimationIdByPlayerId(playerId, AnimationType.left, playerPart),
			AnimationDao.getAnimationIdByPlayerId(playerId, AnimationType.right, playerPart),
			AnimationDao.getAnimationIdByPlayerId(playerId, AnimationType.attack_left, playerPart),
			AnimationDao.getAnimationIdByPlayerId(playerId, AnimationType.attack_right, playerPart));
	}
	
	public static Map<PlayerPartType, AnimationDto> loadAnimationsByPlayerId(int playerId) {
		Map<PlayerPartType, AnimationDto> animationMap = new HashMap<>();
		animationMap.put(PlayerPartType.HEAD, loadAnimationsByPlayerIdPartId(playerId, PlayerPartType.HEAD));
		animationMap.put(PlayerPartType.TORSO, loadAnimationsByPlayerIdPartId(playerId, PlayerPartType.TORSO));
		animationMap.put(PlayerPartType.LEGS, loadAnimationsByPlayerIdPartId(playerId, PlayerPartType.LEGS));
		return animationMap;
	}
	
	public static Map<PlayerPartType, AnimationDto> getEquipmentAnimationsByPlayerId(int playerId) {
		Map<PlayerPartType, AnimationDto> animationMap = new HashMap<>();
		
		final String query = 
				"select " + 
				" player_equipment.player_id," + 
				" player_equipment.equipment_id," + 
				" equipment.player_part_id," + 
				" equipment.equipment_type_id," + 
				" equipment.up_id," + 
				" equipment.down_id," + 
				" equipment.left_id," + 
				" equipment.right_id," + 
				" equipment.attack_left_id," +
				" equipment.attack_right_id" +
				" from player_equipment" + 
				" inner join equipment on equipment.item_id = player_equipment.equipment_id" + 
				" where player_id=?";
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, playerId);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					animationMap.put(PlayerPartType.withValue(rs.getInt("player_part_id")), 
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
		
		return animationMap;
	}
}
