package main.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import main.types.ItemAttributes;
import main.types.StorageTypes;
import main.utils.Utils;

public class PlayerStorageDao {
	private PlayerStorageDao() {}
	
	public static ArrayList<Integer> getStorageListByPlayerId(int playerId, int storageTypeId) {
		ArrayList<Integer> inventoryList = new ArrayList<>();
		final String query = "select item_id from player_storage where player_id=? and storage_id=? order by slot";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, playerId);
			ps.setInt(2, storageTypeId);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					inventoryList.add(rs.getInt("item_id"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return inventoryList;
	}
	
	public static boolean addItemToFirstFreeSlot(int playerId, int storageTypeId, int itemId, int count) {
		ArrayList<Integer> invItemIds = getStorageListByPlayerId(playerId, storageTypeId);
		
		boolean existingStackableSlot = false;
		int slot = -1;
		if (ItemDao.itemHasAttribute(itemId, ItemAttributes.STACKABLE)) {
			slot = invItemIds.indexOf(itemId);// add to an existing stack if exists
			existingStackableSlot = slot != -1;
		}
		
		if (slot == -1)// no existing stack or not stackable; add to first empty slot
			slot = invItemIds.indexOf(0);
		
		if (slot == -1) {
			// no free slots
			return false;
		}
		
		if (existingStackableSlot) {
			addCountToStorageItemSlot(playerId, storageTypeId, slot, count);
		} else {
			return setItemFromPlayerIdAndSlot(playerId, storageTypeId, slot, itemId, count);
		}
		
		return true;
	}
	
	public static HashMap<Integer, InventoryItemDto> getStorageDtoMapByPlayerId(int playerId, int storageTypeId) {
		HashMap<Integer, InventoryItemDto> dtos = new HashMap<>();
		final String query = "select item_id, slot, count from player_storage where player_id=? and storage_id=? order by slot";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, playerId);
			ps.setInt(2, storageTypeId);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					int count = rs.getInt("count");
					String friendlyCount = Utils.getFriendlyCount(count);
					dtos.put(rs.getInt("slot"), new InventoryItemDto(rs.getInt("item_id"), rs.getInt("slot"), count, friendlyCount));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return dtos;
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

	public static ItemDto getItemFromPlayerIdAndSlot(int playerId, int slot) {
		return ItemDao.getItem(getItemIdInSlot(playerId, 1, slot));
	}
	
	public static InventoryItemDto getStorageItemFromPlayerIdAndSlot(int playerId, int storageTypeId, int slot) {
		final String query = "select item_id, slot, count from player_storage where storage_id=? and player_id=? and slot=?";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, storageTypeId);
			ps.setInt(2, playerId);
			ps.setInt(3, slot);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					int count = rs.getInt("count");
					String friendlyCount = Utils.getFriendlyCount(count);
					return new InventoryItemDto(rs.getInt("item_id"), rs.getInt("slot"), count, friendlyCount);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static Integer getItemIdInSlot(int playerId, int storageId, int slot) {
		final String query = "select item_id from player_storage where player_id=? and storage_id=? and slot=?";
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, playerId);
			ps.setInt(2, storageId);
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

	public static boolean setItemFromPlayerIdAndSlot(int playerId, int storageTypeId, int slot, int itemId, int count) {
		final String query = "update player_storage set item_id=?, count=? where player_id=? and storage_id=? and slot=?";
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, itemId);
			ps.setInt(2, count);
			ps.setInt(3, playerId);
			ps.setInt(4, storageTypeId);
			ps.setInt(5, slot);
			return ps.executeUpdate() == 1;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return false;
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
	
	public static boolean clearStorageByPlayerIdStorageTypeId(int playerId, int storageTypeId) {
		final String query = "update player_storage set item_id=0 where player_id=? and storage_id=?";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, playerId);
			ps.setInt(2, storageTypeId);
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

	public static boolean itemExistsInPlayerStorage(int playerId, int itemId) {
		// check inventory, bank, furnace
		final String query = "select item_id from player_storage where player_id=? and item_id=?";
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, playerId);
			ps.setInt(2, itemId);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next())
					return true;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public static void addCountToStorageItemSlot(int playerId, int storageTypeId, int slot, int count) {
		final String query = "update player_storage set count = count + ? where player_id=? and storage_id=? and slot=?";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, count);
			ps.setInt(2, playerId);
			ps.setInt(3, storageTypeId);
			ps.setInt(4, slot);
			ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static void setCountOnInventorySlot(int playerId, int slot, int count) {
		if (count == 0) {
			// should never have a 0-count item; remove it entirely.
			setItemFromPlayerIdAndSlot(playerId, StorageTypes.INVENTORY.getValue(), slot, 0, 1);
			return;
		}
			
		final String query = "update player_storage set count = ? where player_id=? and storage_id=1 and slot=?";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, count);
			ps.setInt(2, playerId);
			ps.setInt(3, slot);
			ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
