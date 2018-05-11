package main.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PlayerSessionDao {
	private static Connection conn;
	public PlayerSessionDao(Connection conn) {
		if (PlayerSessionDao.conn == null)
			PlayerSessionDao.conn = conn;
	}
	
	public static void addPlayer(int id) {
		PreparedStatement ps = null;
		
		try {
			ps = conn.prepareStatement("insert into player_session (player_id) values (?)");
			ps.setInt(1, id);
			ps.executeUpdate();
		} catch (SQLException e) {
			assert(false);
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception e) {}
			}
		}
	}
	
	public static void removePlayer(int id) {
		PreparedStatement ps = null;
		
		try {
			ps = conn.prepareStatement("delete from player_session where player_id = ?");
			ps.setInt(1, id);
			ps.executeUpdate();
		} catch (SQLException e) {
			assert(false);
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception e) {}
			}
		}
	}
	
	public static List<Integer> getActivePlayers() {
		List<Integer> activePlayers = new ArrayList<>();
		PreparedStatement ps = null;
		try {
			ResultSet rs = null;
			ps = conn.prepareStatement("select player_id from player_session");
			rs = ps.executeQuery();
			
			if (rs.next())
				activePlayers.add(rs.getInt("player_id"));
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		} finally {
			if (ps != null)
				try {
					ps.close();
				} catch (Exception e) {}
		}
		return activePlayers;
	}

	public static void setDb(Connection connection) {
		if (PlayerSessionDao.conn == null)
			PlayerSessionDao.conn = connection;
	}
	
	public static void clearAllSessions() {
		PreparedStatement ps = null;
		
		try {
			ps = conn.prepareStatement("delete from player_session");
			ps.executeUpdate();
		} catch (SQLException e) {
			assert(false);
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception e) {}
			}
		}
	}

	public static boolean entryExists(int id) {
		PreparedStatement ps = null;
		try {
			ResultSet rs = null;
			ps = conn.prepareStatement("select count(*) cnt from player_session where player_id = ?");
			ps.setInt(1, id);
			rs = ps.executeQuery();
			
			if (rs.next())
				return rs.getInt("cnt") == 1;
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		} finally {
			if (ps != null)
				try {
					ps.close();
				} catch (Exception e) {}
		}
		
		return true;// weird case, say that the entry does exist
	}
}
