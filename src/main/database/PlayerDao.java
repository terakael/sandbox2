package main.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PlayerDao {
	private static Connection conn;

	public PlayerDao(Connection conn) {
		if (PlayerDao.conn == null)
			PlayerDao.conn = conn;
	}
	
	public PlayerDto getPlayerById(int id) {
		PreparedStatement ps = null;
		try {
			ResultSet rs = null;
			ps = conn.prepareStatement("select id, name, password, posx, posy from player where id = ?");
			ps.setInt(1, id);
			rs = ps.executeQuery();
			
			return new PlayerDto(rs.getInt("id"), rs.getString("name"), rs.getString("password"), rs.getInt("posx"), rs.getInt("posy"));
		} catch (SQLException e) {
			// shit
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception e) {}
			}
		}
		return null;
	}
	
	public PlayerDto getPlayerByUsernameAndPassword(String username, String password) {
		PreparedStatement ps = null;
		try {
			ResultSet rs = null;
			ps = conn.prepareStatement("select id, name, password, posx, posy from player where name = ? and password = ?");
			ps.setString(1, username);
			ps.setString(2, password);
			rs = ps.executeQuery();
			
			if (rs.next())
				return new PlayerDto(rs.getInt("id"), rs.getString("name"), rs.getString("password"), rs.getInt("posx"), rs.getInt("posy"));
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		} finally {
			if (ps != null)
				try {
					ps.close();
				} catch (Exception e) {}
		}
		return null;
	}
	
	public void setDestinationPosition(int id, int x, int y) {
		PreparedStatement ps = null;
		
		try {
			ps = conn.prepareStatement("update player set posx=?, posy=? where id=?");
			ps.setInt(1, x);
			ps.setInt(2, y);
			ps.setInt(3, id);
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

	public void updateLastLoggedIn(int id) {
		PreparedStatement ps = null;
		
		try {
			ps = conn.prepareStatement("update player set last_logged_in=now() where id=?");
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
	
	public static String getNameFromId(int id) {
		PreparedStatement ps = null;
		try {
			ResultSet rs = null;
			ps = conn.prepareStatement("select name from player where id = ?");
			ps.setInt(1, id);
			rs = ps.executeQuery();
			
			if (rs.next())
				return rs.getString("name");
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		} finally {
			if (ps != null)
				try {
					ps.close();
				} catch (Exception e) {}
		}
		return "";
	}

	public static List<PlayerDto> getAllPlayers() {
		List<PlayerDto> playerList = new ArrayList<>();
		PreparedStatement ps = null;
		try {
			ResultSet rs = null;
			ps = conn.prepareStatement("select id, name, posx, posy from player inner join player_session on player_session.player_id = player.id");
			rs = ps.executeQuery();
			
			while (rs.next())
				playerList.add(new PlayerDto(rs.getInt("id"), rs.getString("name"), "", rs.getInt("posx"), rs.getInt("posy")));
		} catch (SQLException e) {
			// shit
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception e) {}
			}
		}
		return playerList;
	}

}
