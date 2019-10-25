package main.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

public class CatchableDao {
	private static HashMap<Integer, Integer> catchables;
	
	public static void cacheCatchables() {
		final String query = "select npc_id, item_id from catchable";
		
		catchables = new HashMap<>();
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					catchables.put(rs.getInt("npc_id"), rs.getInt("item_id"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static boolean isCatchable(int npcId) {
		return catchables.containsKey(npcId);
	}
	
	public static int getCaughtItem(int npcId) {
		if (isCatchable(npcId))
			return catchables.get(npcId);
		return 0;
	}
}
