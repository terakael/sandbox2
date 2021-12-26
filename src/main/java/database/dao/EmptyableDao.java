package database.dao;

import java.util.HashMap;
import java.util.Map;

import database.DbConnection;

public class EmptyableDao {
	private static Map<Integer, Integer> emptyables;
	public static void setupCaches() {
		emptyables = new HashMap<>();
		DbConnection.load("select full_id, empty_id from emptyable", 
				rs -> emptyables.put(rs.getInt("full_id"), rs.getInt("empty_id")));
	}
	
	public static Integer getEmptyFromFull(int fullId) {
		return emptyables.get(fullId);
	}
}
