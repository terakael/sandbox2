package main.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AnimationDao {
	public enum AnimationType {
		up,
		down,
		left,
		right,
		attack
	}
	
	private AnimationDao() {}
	
	private static int getAnimationIdByPlayerId(int playerId, AnimationType type) {
		final String query = "select " + type.name() + "_id from player_animations where player_id=?";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, playerId);
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
	
	public static AnimationDto loadAnimationsByPlayerId(int playerId) {
		AnimationDto animations = new AnimationDto();
		animations.setUp(AnimationDao.getAnimationIdByPlayerId(playerId, AnimationType.up));
		animations.setDown(AnimationDao.getAnimationIdByPlayerId(playerId, AnimationType.down));
		animations.setLeft(AnimationDao.getAnimationIdByPlayerId(playerId, AnimationType.left));
		animations.setRight(AnimationDao.getAnimationIdByPlayerId(playerId, AnimationType.right));
		animations.setAttack(AnimationDao.getAnimationIdByPlayerId(playerId, AnimationType.right));// TODO attack animation
		return animations;
	}
}
