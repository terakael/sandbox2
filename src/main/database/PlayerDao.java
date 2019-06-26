package main.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PlayerDao {
	private PlayerDao() {}
		
	public static PlayerDto getPlayerByUsernameAndPassword(String username, String password) {
		final String query = "select id, name, password, tile_id, current_hp, max_hp, attack_style_id from view_player where name = ? and password = ?";
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
							rs.getInt("current_hp"), 
							rs.getInt("max_hp"), 
							StatsDao.getCombatLevelByPlayerId(rs.getInt("id")), 
							rs.getInt("attack_style_id"), 
							AnimationDao.loadAnimationsByPlayerId(rs.getInt("id")),
							AnimationDao.getEquipmentAnimationsByPlayerId(rs.getInt("id")));
				
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
		final String query = "select name from player where id = ?";
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query)
		) {
			ps.setInt(1, id);
			
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next())
					return rs.getString("name");
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		
		return "";
	}
	
	public static int getIdFromName(String name) {
		final String query = "select id from player where name = ?";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query)
		) {
			ps.setString(1, name);
			
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next())
					return rs.getInt("id");
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		
		return -1;
	}
	
	public static List<PlayerDto> getAllPlayers() {
		List<PlayerDto> playerList = new ArrayList<>();
		final String query = "select id, name, tile_id, current_hp, max_hp, combat_lvl, attack_style_id from view_player inner join player_session on player_session.player_id = view_player.id";
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
			ResultSet rs = ps.executeQuery();
		) {
			while (rs.next()) {
				int playerId = rs.getInt("id");
				playerList.add(new PlayerDto(
						playerId, 
						rs.getString("name"), 
						null, 
						rs.getInt("tile_id"), 
						rs.getInt("current_hp"), 
						rs.getInt("max_hp"), 
						StatsDao.getCombatLevelByPlayerId(playerId),
						rs.getInt("attack_style_id"), 
						AnimationDao.loadAnimationsByPlayerId(playerId),
						AnimationDao.getEquipmentAnimationsByPlayerId(playerId)));
			}
				
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		
		return playerList;
	}
	
	public static void updateTileId(int id, int tileId) {
		final String query = "update player set tile_id=? where id=?";
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query)
		) {
			ps.setInt(1, tileId);
			ps.setInt(2, id);
			
			ps.executeUpdate();
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}
	
	public static HashMap<Integer, String> getAttackStyles() {
		final String query = "select id, name from attack_styles";
		
		HashMap<Integer, String> attackStyleMap = new HashMap<>();
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
			ResultSet rs = ps.executeQuery();
		) {
			while (rs.next()) {
				attackStyleMap.put(rs.getInt("id"), rs.getString("name"));
			}
				
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		
		return attackStyleMap;
	}
	
	public static void updateAttackStyleId(int playerId, int attackStyleId) {
		final String query = "update player set attack_style_id=? where id=?";
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query)
		) {
			ps.setInt(1, attackStyleId);
			ps.setInt(2, playerId);
			
			ps.executeUpdate();
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}
}
