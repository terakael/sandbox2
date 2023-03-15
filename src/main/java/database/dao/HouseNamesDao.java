package database.dao;

import java.util.HashMap;
import java.util.Map;

import database.DbConnection;

public class HouseNamesDao {
	private static Map<Integer, String> houseNamesById;
	
	public static void setupCaches() {
		houseNamesById = new HashMap<>();
		DbConnection.load("select id, name from house_names", rs -> {
			houseNamesById.put(rs.getInt("id"), rs.getString("name"));
		});
	}
	
	public static String getHouseNameById(int id) {
		return houseNamesById.get(id);
	}
}
