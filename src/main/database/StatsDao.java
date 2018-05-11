package main.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class StatsDao {
	private static Connection conn;
	public StatsDao(Connection conn) {
		if (StatsDao.conn == null)
			StatsDao.conn = conn;
	}
	
	public Map<String, Integer> getStatsByPlayerId(int id) {
		try {
			ResultSet rs = null;
			PreparedStatement ps = conn.prepareStatement("select player_id, stat_name, exp from view_player_stats where player_id = ?");
			ps.setInt(1, id);
			rs = ps.executeQuery();
			
			Map<String, Integer> stats = new HashMap<>();
			while (rs.next())
				stats.put(rs.getString("stat_name"), rs.getInt("exp"));
			return stats;
		} catch (SQLException e) {
			// shit
		}
		return null;
	}
}
