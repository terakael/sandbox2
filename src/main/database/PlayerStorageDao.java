package main.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PlayerStorageDao {
	private PlayerStorageDao() {}
	
	public static ArrayList<Integer> getInventoryListByPlayerId(int id) {
		ArrayList<Integer> inventoryList = new ArrayList<>();
		final String query = "select item_id from player_storage where player_id=? and storage_id=1 order by slot";
		
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
	
	public static Integer getStoredCoalByPlayerId(int id) {
		final String query = "select count from player_storage where player_id=? and storage_id=3";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, id);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next())
					return rs.getInt("count");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return 0;
	}

	public static ItemDto getItemFromPlayerIdAndSlot(int id, int slot) {
		final String query = "select items.id, items.name, items.sprite_frame_id, items.leftclick_option, items.other_options from player_storage inner join items on items.id = player_storage.item_id where player_storage.player_id=? and player_storage.storage_id=1 and player_storage.slot=?";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, id);
			ps.setInt(2, slot);
			
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next())
					return new ItemDto(rs.getInt("id"), rs.getString("name"), rs.getInt("sprite_frame_id"), rs.getInt("leftclick_option"), rs.getInt("other_options"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static Integer getItemIdInSlot(int playerId, int inventoryId, int slot) {
		final String query = "select item_id from player_storage where player_id=? and storage_id=? and slot=?";
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, playerId);
			ps.setInt(2, inventoryId);
			ps.setInt(3, slot);
			
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next())
					return rs.getInt("item_id");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static boolean setItemFromPlayerIdAndSlot(int playerId, int slot, int itemId) {
		final String query = "update player_storage set item_id=? where player_id=? and storage_id=1 and slot=?";
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, itemId);
			ps.setInt(2, playerId);
			ps.setInt(3, slot);
			return ps.executeUpdate() == 1;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return false;
	}

	public static boolean addItemByItemIdPlayerId(int playerId, int itemId) {
		
		int freeSlot = getFreeSlotByPlayerId(playerId);
		if (freeSlot == -1) {
			// no free slots
			return false;
		}
		
		return setItemFromPlayerIdAndSlot(playerId, freeSlot, itemId);
	}
	
	public static int getFreeSlotByPlayerId(int playerId) {
		final String query = "select slot from player_storage where player_id=? and storage_id=1 and item_id=0 order by slot limit 1";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query)
		) {
			ps.setInt(1, playerId);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next())
					return rs.getInt("slot");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return -1;
	}
	
	public static boolean clearInventoryByPlayerId(int playerId) {
		final String query = "update player_storage set item_id=0 where player_id=? and storage_id=1";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, playerId);
			return ps.executeUpdate() == 1;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	// checks the item stack count
	public static int getStorageItemCountByPlayerIdItemIdStorageTypeId(int playerId, int itemId, int storageTypeId) {
		final String query = "select count from player_storage where player_id=? and storage_id=? and item_id=?";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, playerId);
			ps.setInt(2, storageTypeId);
			ps.setInt(3, itemId);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next())
					return rs.getInt("count");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	// checks sum of individual slot counts
	public static int getNumStorageItemsByPlayerIdItemIdStorageTypeId(int playerId, int itemId, int storageTypeId) {
		final String query = "select sum(count) cnt from player_storage where player_id=? and storage_id=? and item_id=?";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, playerId);
			ps.setInt(2, storageTypeId);
			ps.setInt(3, itemId);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next())
					return rs.getInt("cnt");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;
	}

	public static boolean addStorageItemIdCountByPlayerIdStorageIdSlotId(int playerId, int storageId, int slot, int count) {
		final String query = "update player_storage set count=count + ? where player_id=? and storage_id=? and slot=?";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, count);
			ps.setInt(2, playerId);
			ps.setInt(3, storageId);
			ps.setInt(4, slot);
			return ps.executeUpdate() == 1;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
		
	}
	
	public static boolean removeAllItemsFromInventoryByPlayerIdItemId(int playerId, int itemId) {
		final String query = "update player_storage set item_id=0 where player_id=? and storage_id=1 and item_id=?";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, playerId);
			ps.setInt(2, itemId);
			return ps.executeUpdate() == 1;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}
}
