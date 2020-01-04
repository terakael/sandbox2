package main.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import main.types.Stats;

public class StatsDao {
	private StatsDao() {}
	
	private static Map<Integer, String> cachedStats = null;
	@Getter private static HashMap<Integer, Integer> expMap = null;
	
	static {
		expMap = new HashMap<>();
		expMap.put(1,  0);

		int cumulativeExp = 0;
		for (int i = 2; i <= 99; ++i) {
			cumulativeExp += (int)(i - 1 + 300 * Math.pow(2, (i - 1) / 7) / 4);
			expMap.put(i, cumulativeExp);
		}
	}
	
	public static Map<Integer, Integer> getStatsByPlayerId(int id) {
		final String query = "select stat_id, exp from player_stats where player_id = ?";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, id);
			
			try (ResultSet rs = ps.executeQuery()) {
				Map<Integer, Integer> stats = new HashMap<>();
				while (rs.next())
					stats.put(rs.getInt("stat_id"), rs.getInt("exp"));
				return stats;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return null;
	}
	
	public static Map<Integer, Integer> getAllStatExpByPlayerId(int id) {
		final String query = "select stat_id, exp from player_stats where player_id = ?";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, id);
			
			try (ResultSet rs = ps.executeQuery()) {
				Map<Integer, Integer> stats = new HashMap<>();
				while (rs.next())
					stats.put(rs.getInt("stat_id"), rs.getInt("exp"));
				return stats;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return null;
	}
	
	public static int getStatLevelByStatIdPlayerId(Stats statId, int playerId) {
		final String query = "select exp from player_stats where stat_id=? and player_id=?";
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, statId.getValue());
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
	
	public static void addExpToPlayer(int playerId, Stats statId, double exp) {
		final String query = "update player_stats set exp=exp+? where player_id=? and stat_id=?";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setDouble(1, exp);
			ps.setInt(2, playerId);
			ps.setInt(3, statId.getValue());
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
	
	public static void setRelativeBoostByPlayerIdStatId(int playerId, Stats statId, int relativeBoost) {
		final String query = "update player_stats set relative_boost=? where player_id=? and stat_id=?";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query)
		) {
			ps.setInt(1, relativeBoost);
			ps.setInt(2, playerId);
			ps.setInt(3, statId.getValue());
			
			ps.executeUpdate();
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}
	
	public static HashMap<Stats, Integer> getRelativeBoostsByPlayerId(int playerId) {
		final String query = "select stat_id, relative_boost from player_stats where player_id=?";
		HashMap<Stats, Integer> relativeBoosts = new HashMap<>();
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query)
		) {
			ps.setInt(1, playerId);
			
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					relativeBoosts.put(Stats.withValue(rs.getInt("stat_id")), rs.getInt("relative_boost"));
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		
		return relativeBoosts;
	}
	
	public static int getCurrentHpByPlayerId(int id) {
		int hp = 0;
		final String query = "select exp, relative_boost as current_boost from player_stats where player_id = ? and stat_id = 5";
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query)
		) {
			ps.setInt(1, id);
			
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next())
					return getLevelFromExp(rs.getInt("exp")) + rs.getInt("current_boost");
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		
		return hp;
	}
	
	public static int getLevelFromExp(int exp) {
		for (Map.Entry<Integer, Integer> entry : expMap.entrySet()) {
			if (exp < entry.getValue())
				return entry.getKey() - 1;
		}
		return 99;
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
		Map<Integer, Integer> stats = getStatsByPlayerId(id);
		
		return getCombatLevelByStats(
			getLevelFromExp(stats.get(Stats.STRENGTH.getValue())),
			getLevelFromExp(stats.get(Stats.ACCURACY.getValue())),
			getLevelFromExp(stats.get(Stats.DEFENCE.getValue())),
			getLevelFromExp(stats.get(Stats.AGILITY.getValue())),
			getLevelFromExp(stats.get(Stats.HITPOINTS.getValue())),
			getLevelFromExp(stats.get(Stats.MAGIC.getValue()))
		);
	}
	
	public static int getCombatLevelByStats(int str, int att, int def, int agil, int hp, int magic) {
		return (int)Math.ceil(((double)str + att) / 4) 
				+ (int)Math.ceil(((double)def + agil) / 5) 
				+ (int)Math.ceil(((double)hp + magic) / 6);
	}
	
	public static int getCombatLevelByStats(HashMap<Stats, Integer> stats) {
		return getCombatLevelByStats(
				stats.get(Stats.STRENGTH),
				stats.get(Stats.ACCURACY),
				stats.get(Stats.DEFENCE),
				stats.get(Stats.AGILITY),
				stats.get(Stats.HITPOINTS),
				stats.get(Stats.MAGIC)
			);
	}
}
