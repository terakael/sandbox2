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

	public static ItemDto getItemFromPlayerIdAndSlot(int id, int slot) {
		final String query = "select items.id, items.name, items.description, items.sprite_frame_id, items.context_options from player_inventories inner join items on items.id = player_inventories.item_id where player_inventories.player_id=? and player_inventories.slot=?";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, id);
			ps.setInt(2, slot);
			
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next())
					return new ItemDto(rs.getInt("id"), rs.getString("name"), rs.getString("description"), rs.getInt("sprite_frame_id"), rs.getInt("context_options"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return null;
	}

	public static void setItemFromPlayerIdAndSlot(int playerId, int slot, int itemId) {
		final String query = "update player_inventories set item_id=? where player_id=? and slot=?";
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, itemId);
			ps.setInt(2, playerId);
			ps.setInt(3, slot);
			ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
