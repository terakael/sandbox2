package main.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class ConsumableDao {
	private static ArrayList<ConsumableDto> consumables;
	
	public static void cacheConsumables() {
		final String query = "select item_id, stat_id, amount from consumable";
		
		consumables = new ArrayList<>();
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					consumables.add(new ConsumableDto(rs.getInt("item_id"), rs.getInt("stat_id"), rs.getInt("amount")));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static ArrayList<ConsumableDto> getConsumablesByItemId(int itemId) {
		ArrayList<ConsumableDto> consumablesByItemId = new ArrayList<>();
		for (ConsumableDto dto : consumables) {
			if (dto.getItemId() == itemId)
				consumablesByItemId.add(dto);
		}
		return consumablesByItemId;
	}
}
