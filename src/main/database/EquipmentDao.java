package main.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class EquipmentDao {
	private EquipmentDao() {};
	
	public static List<Integer> getEquippedSlotsByPlayerId(int id) {
		List<Integer> equippedSlots = new ArrayList<>();
		final String query = "select slot from player_equipment where player_id=? order by slot";
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query)
		) {
			ps.setInt(1, id);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					equippedSlots.add(rs.getInt("slot"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return equippedSlots;
	}

	public static boolean isSlotEquipped(int playerId, int slot) {
		final String query = "select slot from player_equipment where player_id=? and slot=?";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query)
		) {
			ps.setInt(1, playerId);
			ps.setInt(2, slot);
			
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	public static void clearEquippedItem(int playerId, int slot) {
		final String query = "delete from player_equipment where player_id=? and slot=?";
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, playerId);
			ps.setInt(2, slot);
			ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static void setEquippedItem(int playerId, int slot, int itemId) {
		final String query = "insert into player_equipment (player_id, equipment_id, slot) values (?, ?, ?)";
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query)
		) {
			ps.setInt(1, playerId);
			ps.setInt(2, itemId);
			ps.setInt(3, slot);
			ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static EquipmentDto getEquipmentByItemId(int itemId) {
		final String query = "select item_id, player_part_id from equipment where item_id=?";
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query)
		) {
			ps.setInt(1, itemId);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next())
					return new EquipmentDto(rs.getInt("item_id"), rs.getInt("player_part_id"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void clearEquippedItemByPartId(int playerId, int partId) {
		final String query = 
				"delete player_equipment from player_equipment " + 
				"inner join equipment on equipment.item_id=player_equipment.equipment_id " + 
				"where player_id=? and player_part_id=?";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, playerId);
			ps.setInt(2, partId);
			ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static void clearAllEquppedItems(int playerId) {
		final String query = 
				"delete player_equipment from player_equipment " + 
				"inner join equipment on equipment.item_id=player_equipment.equipment_id " + 
				"where player_id=?";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, playerId);
			ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static boolean isItemEquippedByItemIdAndSlot(int playerId, int itemId, int slot) {
		final String query = "select slot from player_equipment where player_id=? and equipment_id=? and slot=?";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, playerId);
			ps.setInt(2, itemId);
			ps.setInt(3, slot);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next())
					return true;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	public static EquipmentBonusDto getEquipmentBonusesByPlayerId(int playerId) {
		final String query = 
				"select sum(acc) acc, sum(str) str, sum(def) def, sum(agil) agil " + 
				"from player_equipment " + 
				"inner join equipment on equipment.item_id = player_equipment.equipment_id " + 
				"where player_equipment.player_id = ?";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, playerId);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next())
					return new EquipmentBonusDto(rs.getInt("acc"), rs.getInt("str"), rs.getInt("def"), rs.getInt("agil"), 0);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return new EquipmentBonusDto(0, 0, 0, 0, 0);
	}
} 