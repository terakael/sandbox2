package main.database.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import main.database.DbConnection;

public class BuryableDao {
	private static Map<Integer, Integer> buryables = null;
	
	public static void setupCaches() {
		cacheBuryables();
	}
	
	private static void cacheBuryables() {
		final String query = "select item_id, exp from buryable";
		
		buryables = new HashMap<>();
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					buryables.put(rs.getInt("item_id"), rs.getInt("exp"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static boolean isBuryable(int itemId) {
		return buryables.containsKey(itemId);
	}
	
	public static int getExpFromBuryable(int itemId) {
		if (!isBuryable(itemId))
			return 0;
		return buryables.get(itemId);
	}
}
