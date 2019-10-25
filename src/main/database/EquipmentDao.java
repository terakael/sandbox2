package main.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import main.types.EquipmentTypes;
import main.types.Items;
import main.types.PlayerPartType;

public class EquipmentDao {
	private EquipmentDao() {};
	
	private static HashMap<Integer, EquipmentTypes> equipmentByType = new HashMap<>();
	private static HashMap<Integer, EquipmentDto> equipment = new HashMap<>();
	private static HashMap<Items, Items> reinforcedToBase = new HashMap<>();// reinforced_id, base_id
	
	public static void setupCaches() {
		cacheEquipmentByType();
		cacheEquipment();
		cacheReinforcedtoBaseMap();
	}
	
	private static void cacheEquipment() {
		final String query = "select item_id, player_part_id, requirement from equipment";
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query)
		) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					int itemId = rs.getInt("item_id");
					equipment.put(itemId, new EquipmentDto(itemId, rs.getInt("player_part_id"), rs.getInt("requirement"), getEquipmentTypeByEquipmentId(itemId)));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	private static void cacheEquipmentByType() {
		final String query = 
				"select equipment_types.id, equipment.item_id from equipment" + 
				" inner join equipment_types on equipment.equipment_type_id = equipment_types.id";
		
		try (
				Connection connection = DbConnection.get();
				PreparedStatement ps = connection.prepareStatement(query)
			) {
				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						EquipmentTypes type = EquipmentTypes.withValue(rs.getInt("id"));
						if (type != null)
							equipmentByType.put(rs.getInt("item_id"), type);
					}
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
	}
	
	private static void cacheReinforcedtoBaseMap() {
		reinforcedToBase.put(Items.REINFORCED_COPPER_HELMET, Items.COPPER_HELMET);
		reinforcedToBase.put(Items.REINFORCED_COPPER_PLATEBODY, Items.COPPER_PLATEBODY);
		reinforcedToBase.put(Items.REINFORCED_COPPER_PLATELEGS, Items.COPPER_PLATELEGS);
		reinforcedToBase.put(Items.REINFORCED_COPPER_SHIELD, Items.COPPER_SHIELD);
		
		reinforcedToBase.put(Items.REINFORCED_IRON_HELMET, Items.IRON_HELMET);
		reinforcedToBase.put(Items.REINFORCED_IRON_PLATEBODY, Items.IRON_PLATEBODY);
		reinforcedToBase.put(Items.REINFORCED_IRON_PLATELEGS, Items.IRON_PLATELEGS);
		reinforcedToBase.put(Items.REINFORCED_IRON_SHIELD, Items.IRON_SHIELD);
		
		reinforcedToBase.put(Items.REINFORCED_STEEL_HELMET, Items.STEEL_HELMET);
		reinforcedToBase.put(Items.REINFORCED_STEEL_PLATEBODY, Items.STEEL_PLATEBODY);
		reinforcedToBase.put(Items.REINFORCED_STEEL_PLATELEGS, Items.STEEL_PLATELEGS);
		reinforcedToBase.put(Items.REINFORCED_STEEL_SHIELD, Items.STEEL_SHIELD);
		
		reinforcedToBase.put(Items.REINFORCED_MITHRIL_HELMET, Items.MITHRIL_HELMET);
		reinforcedToBase.put(Items.REINFORCED_MITHRIL_PLATEBODY, Items.MITHRIL_PLATEBODY);
		reinforcedToBase.put(Items.REINFORCED_MITHRIL_PLATELEGS, Items.MITHRIL_PLATELEGS);
		reinforcedToBase.put(Items.REINFORCED_MITHRIL_SHIELD, Items.MITHRIL_SHIELD);
		
		reinforcedToBase.put(Items.REINFORCED_ADDY_HELMET, Items.ADDY_HELMET);
		reinforcedToBase.put(Items.REINFORCED_ADDY_PLATEBODY, Items.ADDY_PLATEBODY);
		reinforcedToBase.put(Items.REINFORCED_ADDY_PLATELEGS, Items.ADDY_PLATELEGS);
		reinforcedToBase.put(Items.REINFORCED_ADDY_SHIELD, Items.ADDY_SHIELD);
		
		reinforcedToBase.put(Items.REINFORCED_RUNE_HELMET, Items.RUNE_HELMET);
		reinforcedToBase.put(Items.REINFORCED_RUNE_PLATEBODY, Items.RUNE_PLATEBODY);
		reinforcedToBase.put(Items.REINFORCED_RUNE_PLATELEGS, Items.RUNE_PLATELEGS);
		reinforcedToBase.put(Items.REINFORCED_RUNE_SHIELD, Items.RUNE_SHIELD);
	}
	
	public static int getBaseItemFromReinforcedItem(int reinforcedId) {
		Items reinforcedItem = Items.withValue(reinforcedId);
		if (reinforcedItem == null)
			return 0;
		
		if (reinforcedToBase.containsKey(reinforcedItem))
			return reinforcedToBase.get(reinforcedItem).getValue();
		
		return 0;
	}
	
	public static EquipmentTypes getEquipmentTypeByEquipmentId(int equipmentId) {
		if (equipmentByType.containsKey(equipmentId))
			return equipmentByType.get(equipmentId);
		return null;
	}
	
	public static HashSet<Integer> getEquippedSlotsByPlayerId(int id) {
		HashSet<Integer> equippedSlots = new HashSet<>();
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
	
	public static HashMap<Integer, Integer> getEquippedSlotsAndItemIdsByPlayerId(int id) {
		HashMap<Integer, Integer> equipped = new HashMap<>();
		final String query = "select equipment_id, slot from player_equipment where player_id=?";
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query)
		) {
			ps.setInt(1, id);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					equipped.put(rs.getInt("equipment_id"), rs.getInt("slot"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return equipped;
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
		if (equipment.containsKey(itemId))
			return equipment.get(itemId);
		return null;
	}
	
	public static int getWeaponIdByPlayerId(int playerId) {
		final String query = "select equipment_id from view_player_equipment where player_id=? and player_part_id=4";// 4 = onhand
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query)
		) {
			ps.setInt(1, playerId);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next())
					return rs.getInt("equipment_id");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;
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
				"select sum(acc) acc, sum(str) str, sum(def) def, sum(agil) agil, sum(mage) mage, sum(hp) hp, sum(speed) speed " + 
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
					return new EquipmentBonusDto(rs.getInt("acc"), rs.getInt("str"), rs.getInt("def"), rs.getInt("agil"), rs.getInt("mage"), rs.getInt("hp"), rs.getInt("speed"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return new EquipmentBonusDto(0, 0, 0, 0, 0, 0, 0);
	}
} 