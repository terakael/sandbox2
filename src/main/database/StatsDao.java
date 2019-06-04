package main.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class StatsDao {
	private StatsDao() {}
	
	private static Map<Integer, String> cachedStats = null;
	
	public static Map<String, Integer> getStatsByPlayerId(int id) {
		final String query = "select player_id, stat_name, exp from view_player_stats where player_id = ?";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, id);
			
			try (ResultSet rs = ps.executeQuery()) {
				Map<String, Integer> stats = new HashMap<>();
				while (rs.next())
					stats.put(rs.getString("stat_name"), rs.getInt("exp"));
				return stats;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return null;
	}
	
	public static int getStatLevelByStatIdPlayerId(int statId, int playerId) {
		final String query = "select exp from player_stats where stat_id=? and player_id=?";
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, statId);
			ps.setInt(2, playerId);
			
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next())
					return getLevelFromExp(rs.getInt("exp"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return -1;
	}
	
	public static void addExpToPlayer(int playerId, int statId, int exp) {
		final String query = "update player_stats set exp=exp+? where player_id=? and stat_id=?";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, exp);
			ps.setInt(2, playerId);
			ps.setInt(3, statId);
			ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static Map<Integer, String> getStats() {
		final String query = "select id, short_name from stats";
		
		Map<Integer, String> stats = new HashMap<>();
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
			ResultSet rs = ps.executeQuery()
		) {
			while (rs.next())
				stats.put(rs.getInt("id"), rs.getString("short_name"));
			return stats;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static int getStatIdByName(String stat) {
		if (StatsDao.cachedStats == null) {
			StatsDao.cachedStats = StatsDao.getStats();
			if (StatsDao.cachedStats == null)
				return -1;
		}
		
		for (Map.Entry<Integer, String> entry : StatsDao.cachedStats.entrySet()) {
			if (entry.getValue().equals(stat))
				return entry.getKey();
		}
		
		return -1;
	}
	
	public static String getStatShortNameByStatId(int id) {
		if (StatsDao.cachedStats == null) {
			StatsDao.cachedStats = StatsDao.getStats();
			if (StatsDao.cachedStats == null)
				return null;
		}
		
		if (StatsDao.cachedStats.containsKey(id))
			return StatsDao.cachedStats.get(id);
		
		return null;
	}
	
	public static void setRelativeBoostByPlayerIdStatId(int playerId, int statId, int relativeBoost) {
		final String query = "update player_stats set relative_boost=? where player_id=? and stat_id=?";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query)
		) {
			ps.setInt(1, relativeBoost);
			ps.setInt(2, playerId);
			ps.setInt(3, statId);
			
			ps.executeUpdate();
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}
	
	public static int getCurrentHpByPlayerId(int id) {
		int hp = 0;
		final String query = "select floor(sqrt(exp)) + relative_boost as current_boost from player_stats where player_id = ? and stat_id = 5";
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query)
		) {
			ps.setInt(1, id);
			
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next())
					return rs.getInt("current_boost");
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		
		return hp;
	}
	
	public static int getLevelFromExp(int exp) {
		return (int)Math.sqrt(exp);
	}

	public static int getMaxHpByPlayerId(int id) {
		final String query = "select exp from player_stats where player_id = ? and stat_id = 5";
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query)
		) {
			ps.setInt(1, id);
			
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next())
					return getLevelFromExp(rs.getInt("exp"));
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		
		return 0;
	}
	
	public static int getCombatLevelByPlayerId(int id) {
		Map<String, Integer> stats = getStatsByPlayerId(id);
		
		return getCombatLevelByStats(
			getLevelFromExp(stats.get("strength")),
			getLevelFromExp(stats.get("accuracy")),
			getLevelFromExp(stats.get("defence")),
			getLevelFromExp(stats.get("agility")),
			getLevelFromExp(stats.get("hitpoints")),
			getLevelFromExp(stats.get("magic"))
		);
	}
	
	public static int getCombatLevelByStats(int str, int att, int def, int agil, int hp, int magic) {
		return ((str + att) / 2) + ((def + agil) / 3) + ((hp + magic) / 4);
	}
}
