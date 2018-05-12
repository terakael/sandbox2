package main.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PlayerDao {
	private PlayerDao() {}
	
	public static PlayerDto getPlayerById(int id) throws SQLException {
		final String query = "select id, name, password, posx, posy from player where id = ?";
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query)
		) {
			ps.setInt(1, id);
			
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next())
					return new PlayerDto(rs.getInt("id"), rs.getString("name"), rs.getString("password"), rs.getInt("posx"), rs.getInt("posy"));
				return null;
			}
		}
	}
	
	public static PlayerDto getPlayerByUsernameAndPassword(String username, String password) {
		final String query = "select id, name, password, posx, posy from player where name = ? and password = ?";
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query)
		) {
			ps.setString(1, username);
			ps.setString(2, password);
			
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next())
					return new PlayerDto(rs.getInt("id"), rs.getString("name"), rs.getString("password"), rs.getInt("posx"), rs.getInt("posy"));
				return null;
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		return null;
	}
	
	public static void setDestinationPosition(int id, int x, int y) {
		final String query = "update player set posx=?, posy=? where id=?";
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query)
		) {
			ps.setInt(1, x);
			ps.setInt(2, y);
			ps.setInt(3, id);
			
			ps.executeUpdate();
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
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
	
	public static List<PlayerDto> getAllPlayers() {
		List<PlayerDto> playerList = new ArrayList<>();
		final String query = "select id, name, posx, posy from player inner join player_session on player_session.player_id = player.id";
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
			ResultSet rs = ps.executeQuery();
		) {
			while (rs.next())
				playerList.add(new PlayerDto(rs.getInt("id"), rs.getString("name"), null, rs.getInt("posx"), rs.getInt("posy")));
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		
		return playerList;
	}
}
