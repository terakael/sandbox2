package main.database.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import main.database.DbConnection;
import main.database.dto.SmithableDto;

public class SmithableDao {
	private static Map<Integer, SmithableDto> smithables;
	
	public static void setupCaches() {
		smithables = new HashMap<>();
		
		final String query = "select item_id, name, level, material_1, material1_name, count_1, material_2, material2_name, count_2, material_3, material3_name, count_3 from view_smithable";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					smithables.put(rs.getInt("item_id"), new SmithableDto(
							rs.getInt("item_id"),
							rs.getString("name"),
							rs.getInt("level"),
							rs.getInt("material_1"),
							rs.getString("material1_name"),
							rs.getInt("count_1"),
							rs.getInt("material_2"),
							rs.getString("material2_name"),
							rs.getInt("count_2"),
							rs.getInt("material_3"),
							rs.getString("material3_name"),
							rs.getInt("count_3")
					));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static List<SmithableDto> getAllItemsThatUseMaterial(int materialId) {
		return smithables.values().stream().
				filter(e -> e.getMaterial1() == materialId || e.getMaterial2() == materialId || e.getMaterial3() == materialId)
				.collect(Collectors.toList());
	}
	
	public static SmithableDto getSmithableItemByItemId(int itemId) {
		return smithables.get(itemId);
	}
}
