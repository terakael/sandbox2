package database.dao;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import database.DbConnection;
import database.dto.ShipDto;

public class ShipDao {
	private static Map<Integer, ShipDto> shipsById = new HashMap<>();
	
	public static void setupCaches() {
		DbConnection.load("select hull_scenery_id, name, slot_size, up_id, down_id, left_id, right_id from ships", rs -> {
			shipsById.put(rs.getInt("hull_scenery_id"), 
				new ShipDto(
					rs.getInt("hull_scenery_id"), 
					rs.getString("name"), 
					rs.getInt("slot_size"), 
					rs.getInt("up_id"), 
					rs.getInt("down_id"), 
					rs.getInt("left_id"), 
					rs.getInt("right_id"),
					1,
					0));
		});
	}
	
	public static ShipDto getDtoById(int id) {
		return shipsById.get(id);
	}
	
	public static Set<ShipDto> getShipDtos() {
		return new HashSet<>(shipsById.values());
	}
}
