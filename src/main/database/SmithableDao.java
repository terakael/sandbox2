package main.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class SmithableDao {
	public static ArrayList<SmithableDto> getAllItemsThatUseMaterial(int materialId) {
		final String query = 
		"select item_id, name, level, material_1, material1_name, count_1, material_2, material2_name, count_2, material_3, material3_name, count_3"
		+ " from view_smithable"
		+ " where material_1=? or material_2=? or material_3=?";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, materialId);
			ps.setInt(2, materialId);
			ps.setInt(3, materialId);
			
			try (ResultSet rs = ps.executeQuery()) {
				ArrayList<SmithableDto> smithables = new ArrayList<>();
				while (rs.next())
					smithables.add(new SmithableDto(
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
				return smithables;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return null;
	}
}
