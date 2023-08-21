package database.dao;

import java.util.HashSet;
import java.util.Set;

import database.DbConnection;
import database.dto.ShipAccessoryDto;
import lombok.Getter;

public class ShipAccessoryDao {
	@Getter private static Set<ShipAccessoryDto> shipAccessories = new HashSet<>();
	
	public static void setupCaches() {
		final String query = "select id, name, sprite_frame_id, level, primary_material_id, primary_material_count, secondary_material_id, secondary_material_count, offense, defense, fishing, storage, crew from ship_accessories";
		DbConnection.load(query, rs -> shipAccessories.add(
				new ShipAccessoryDto(
						rs.getInt("id"),
						rs.getString("name"),
						rs.getInt("sprite_frame_id"),
						rs.getInt("level"),
						rs.getInt("primary_material_id"),
						rs.getInt("primary_material_count"),
						rs.getInt("secondary_material_id"),
						rs.getInt("secondary_material_count"),
						rs.getInt("offense"),
						rs.getInt("defense"),
						rs.getInt("fishing"),
						rs.getInt("storage"),
						rs.getInt("crew"))));
	}
	
	public static ShipAccessoryDto getAccessoryById(int id) {
		return shipAccessories.stream()
				.filter(e -> e.getId() == id)
				.findFirst()
				.orElse(null);
	}
}
