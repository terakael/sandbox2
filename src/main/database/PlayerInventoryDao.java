package main.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PlayerInventoryDao {
	private PlayerInventoryDao() {}
	
	public static List<Integer> getInventoryListByPlayerId(int id) {
		List<Integer> inventoryList = new ArrayList<>();
		final String query = "select item_id from player_inventories where player_id=? order by slot";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, id);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					inventoryList.add(rs.getInt("item_id"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return inventoryList;
	}
}
