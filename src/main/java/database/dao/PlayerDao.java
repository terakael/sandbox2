package database.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import database.DbConnection;
import database.dto.PlayerDto;
import lombok.Getter;
import processing.WorldProcessor;
import processing.attackable.Player;

public class PlayerDao {
	@Getter private static Map<Integer, String> attackStyles = new HashMap<>();
	
	private PlayerDao() {}
	
	public static void setupCaches() {
		cacheAttackStyles();
	}
		
	public static PlayerDto getPlayerByUsernameAndPassword(String username, String password) {
		final String query = "select id, name, password, tile_id, floor, attack_style_id from player where name = ? and password = ?";
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query)
		) {
			ps.setString(1, username);
			ps.setString(2, password);
			
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next())					
					return new PlayerDto(
							rs.getInt("id"), 
							rs.getString("name"), 
							rs.getString("password"), 
							rs.getInt("tile_id"),
							rs.getInt("floor"),
							StatsDao.getCurrentHpByPlayerId(rs.getInt("id")), 
							StatsDao.getMaxHpByPlayerId(rs.getInt("id")), 
							StatsDao.getCurrentPrayerByPlayerId(rs.getInt("id")),
							StatsDao.getCombatLevelByPlayerId(rs.getInt("id")), 
							rs.getInt("attack_style_id"), 
							PlayerBaseAnimationsDao.getBaseAnimationsBasedOnEquipmentTypes(rs.getInt("id")),
							EquipmentDao.getEquipmentAnimationsByPlayerId(rs.getInt("id")),
							EquipmentDao.getEquipmentTypeByEquipmentId(EquipmentDao.getWeaponIdByPlayerId(rs.getInt("id"))));
				
				return null;
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		return null;
	}
	
	public static void updateLastLoggedIn(int id) {
		final String query = "update player set last_logged_in=now() where id=?";
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query)
		) {
			ps.setInt(1, id);
			ps.executeUpdate();
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}
	
	public static String getNameFromId(int id) {
		Optional<Player> player = WorldProcessor.playerSessions.values().stream().filter(e -> e.getId() == id).findFirst();
		return player.isPresent() ? player.get().getDto().getName() : "";
	}
	
	public static int getIdFromName(String name) {
		Optional<Player> player = WorldProcessor.playerSessions.values().stream().filter(e -> e.getDto().getName().equals(name)).findFirst();
		return player.isPresent() ? player.get().getId() : -1;
	}
	
	public static void cacheAttackStyles() {
		DbConnection.load("select id, name from attack_styles", 
				rs -> attackStyles.put(rs.getInt("id"), rs.getString("name")));
	}
}
