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
	private static Map<Integer, String> playerNamesById;
	
	private PlayerDao() {}
	
	public static void setupCaches() {
		playerNamesById = new HashMap<>();
		DbConnection.load("select id, name from player", rs -> {
			playerNamesById.put(rs.getInt("id"), rs.getString("name"));
		});
	}
	
	public static PlayerDto getPlayerByUsernameAndPassword(String username, String password) {
		final String query = "select id, name, password, tile_id, floor, house_id, attack_style_id from player where name = ? and password = ?";
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
							rs.getInt("house_id"),
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
	
	public static String getNameFromId(int id) {
		if (playerNamesById.containsKey(id))
			return playerNamesById.get(id);
		return "";
	}
	
	public static int getIdFromName(String name) {
		return playerNamesById.entrySet().stream()
				.filter(entry -> entry.getValue().equals(name))
				.map(e -> e.getKey())
				.findFirst()
				.orElse(-1);
	}
}
