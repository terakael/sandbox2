package main.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PlayerSessionDao {
	private PlayerSessionDao() {}
	
	public static void addPlayer(int id) {
		final String query = "insert into player_session (player_id) values (?)";
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query)
		) {
			ps.setInt(1, id);
			ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static void removePlayer(int id) {
		final String query = "delete from player_session where player_id = ?";
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query)
		) {
			ps.setInt(1, id);
			ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static List<Integer> getActivePlayers() {
		List<Integer> activePlayers = new ArrayList<>();
		final String query = "select player_id from player_session";
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
			ResultSet rs = ps.executeQuery();	
		) {
			while (rs.next())
				activePlayers.add(rs.getInt("player_id"));
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return activePlayers;
	}
	
	public static void clearAllSessions() {
		final String query = "delete from player_session";
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query)
		) {
			ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static boolean entryExists(int id) {
		final String query = "select count(*) cnt from player_session where player_id = ?";
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query)
		) {
			ps.setInt(1, id);
			
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next())
					return rs.getInt("cnt") == 1;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		assert(false);
		return true;// weird case, say that the entry does exist
	}
}
