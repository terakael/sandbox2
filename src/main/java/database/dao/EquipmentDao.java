package database.dao;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import database.DbConnection;
import database.dto.EquipmentBonusDto;
import database.dto.EquipmentDto;
import database.dto.PlayerAnimationDto;
import database.entity.delete.DeletePlayerEquipment;
import database.entity.insert.InsertPlayerEquipment;
import lombok.Getter;
import processing.managers.DatabaseUpdater;
import types.EquipmentTypes;
import types.PlayerPartType;

public class EquipmentDao {
	private EquipmentDao() {};
	
	private static HashMap<Integer, EquipmentTypes> equipmentByType = new HashMap<>();
	private static HashMap<Integer, EquipmentDto> equipment = new HashMap<>();
	
	@Getter private static Map<Integer, HashMap<Integer, Integer>> playerEquipment; // playerId, <equipmentId, slot>
	
	public static void setupCaches() {
		cacheEquipmentByType();
		cacheEquipment();
		cachePlayerEquipment();
	}
	
	private static void cacheEquipment() {
		final String query = "select item_id, player_part_id, animation_id, color, requirement, acc, str, def, pray, mage, hp, speed from equipment";
		DbConnection.load(query, rs -> {
			final int itemId = rs.getInt("item_id");
			equipment.put(itemId, 
				new EquipmentDto(itemId, 
								 rs.getInt("player_part_id"), 
								 rs.getInt("requirement"), 
								 getEquipmentTypeByEquipmentId(itemId),
								 new PlayerAnimationDto(AnimationDao.getAnimationDtoById(rs.getInt("animation_id")), rs.getInt("color") == 0 ? null : rs.getInt("color")),
								 new EquipmentBonusDto(rs.getInt("acc"), rs.getInt("str"), rs.getInt("def"), rs.getInt("pray"), rs.getInt("mage"), rs.getInt("hp"), rs.getInt("speed"))));
		});
	}
	
	private static void cacheEquipmentByType() {
		final String query = 
				"select equipment_types.id, equipment.item_id from equipment" + 
				" inner join equipment_types on equipment.equipment_type_id = equipment_types.id";
		
		DbConnection.load(query, rs -> {
			EquipmentTypes type = EquipmentTypes.withValue(rs.getInt("id"));
			if (type != null)
				equipmentByType.put(rs.getInt("item_id"), type);
		});
	}
	
	public static EquipmentTypes getEquipmentTypeByEquipmentId(int equipmentId) {
		if (equipmentByType.containsKey(equipmentId))
			return equipmentByType.get(equipmentId);
		return null;
	}
	
	public static Set<Integer> getEquippedSlotsByPlayerId(int playerId) {
		if (!playerEquipment.containsKey(playerId))
			return new HashSet<>();
		
		return new HashSet<>(playerEquipment.get(playerId).values());
	}
	
	public static Map<Integer, Integer> getEquippedSlotsAndItemIdsByPlayerId(int playerId) {
		if (!playerEquipment.containsKey(playerId))
			return new HashMap<>();
		
		return playerEquipment.get(playerId);
	}

	public static boolean isSlotEquipped(int playerId, int slot) {
		if (!playerEquipment.containsKey(playerId))
			return false;
		
		return playerEquipment.get(playerId).values().contains(slot);
	}
	
	public static boolean isItemEquipped(int playerId, int itemId) {
		if (!playerEquipment.containsKey(playerId))
			return false;
		
		return playerEquipment.get(playerId).keySet().contains(itemId);
	}

	public static void clearEquippedItem(int playerId, int slot) {
		int equippedItemIdToRemove = -1;
		if (playerEquipment.containsKey(playerId)) {
			for (Map.Entry<Integer, Integer> entry : playerEquipment.get(playerId).entrySet()) {
				if (entry.getValue() == slot) {
					equippedItemIdToRemove = entry.getKey();
					break;
				}
			}
		}
		
		if (equippedItemIdToRemove != -1) {
			playerEquipment.get(playerId).remove(equippedItemIdToRemove);
			DatabaseUpdater.enqueue(DeletePlayerEquipment.builder().playerId(playerId).equipmentId(equippedItemIdToRemove).build());
		}
	}
	
	public static void setEquippedItem(int playerId, int slot, int itemId) {
		if (!playerEquipment.containsKey(playerId))
			playerEquipment.put(playerId, new HashMap<>());
		
		if (!playerEquipment.get(playerId).containsKey(itemId))
			playerEquipment.get(playerId).put(itemId, slot);
		
		DatabaseUpdater.enqueue(InsertPlayerEquipment.builder().playerId(playerId).equipmentId(itemId).slot(slot).build());
	}
	
	public static EquipmentDto getEquipmentByItemId(int itemId) {
		if (equipment.containsKey(itemId))
			return equipment.get(itemId);
		return null;
	}
	
	public static int getWeaponIdByPlayerId(int playerId) {
		if (!playerEquipment.containsKey(playerId))
			return 0;
		
		for (int equipmentId : playerEquipment.get(playerId).keySet()) {
			if (equipment.get(equipmentId).getPartId() == PlayerPartType.ONHAND.getValue()) {
				return equipmentId;
			}
		}
		
		return 0;
	}
	
	public static void clearEquippedItemByPartId(int playerId, int partId) {
		if (!playerEquipment.containsKey(playerId))
			return;
		
		int equipmentIdToRemove = -1;
		for (int equipmentId : playerEquipment.get(playerId).keySet()) {
			if (equipment.get(equipmentId).getPartId() == partId) {
				equipmentIdToRemove = equipmentId;
				break;
			}
		}
		
		if (equipmentIdToRemove != -1) {
			playerEquipment.get(playerId).remove(equipmentIdToRemove);
			DatabaseUpdater.enqueue(DeletePlayerEquipment.builder().playerId(playerId).equipmentId(equipmentIdToRemove).build());
		}
	}
	
	public static void clearAllEquppedItems(int playerId) {
		if (playerEquipment.containsKey(playerId))
			playerEquipment.remove(playerId);
		
		DatabaseUpdater.enqueue(DeletePlayerEquipment.builder().playerId(playerId).build());
	}

	public static boolean isItemEquippedByItemIdAndSlot(int playerId, int itemId, int slot) {
		if (!playerEquipment.containsKey(playerId))
			return false;
		
		if (!playerEquipment.get(playerId).containsKey(itemId))
			return false;
		
		return playerEquipment.get(playerId).get(itemId) == slot;
	}
	
	public static EquipmentBonusDto getEquipmentBonusesByPlayerId(int playerId) {
		EquipmentBonusDto totalBonuses = new EquipmentBonusDto(0, 0, 0, 0, 0, 0, 0);
		
		if (!playerEquipment.containsKey(playerId))
			return totalBonuses;
		
		for (int equipmentId : playerEquipment.get(playerId).keySet()) {
			if (!equipment.containsKey(equipmentId))
				continue;
			totalBonuses.add(equipment.get(equipmentId).getBonuses());
		}
		
		return totalBonuses;
	}
	
	public static void cachePlayerEquipment() {
		playerEquipment = new HashMap<>();
		
		final String query = "select player_id, equipment_id, slot from player_equipment";
		DbConnection.load(query, rs -> {
			final int playerId = rs.getInt("player_id");
			final int slot = rs.getInt("slot");
			final int equipmentId = rs.getInt("equipment_id");
			playerEquipment.putIfAbsent(playerId, new HashMap<>());
			playerEquipment.get(playerId).put(equipmentId, slot);
		});
	}

	public static Map<PlayerPartType, PlayerAnimationDto> getEquipmentAnimationsByPlayerId(int playerId) {
		Map<PlayerPartType, PlayerAnimationDto> animationMap = new HashMap<>();
		
		if (!playerEquipment.containsKey(playerId))
			return animationMap;
		
		for (int equipmentId : playerEquipment.get(playerId).keySet()) {
			final EquipmentDto dto = equipment.get(equipmentId);
			animationMap.put(PlayerPartType.withValue(dto.getPartId()), dto.getAnimations());
		}
		
		return animationMap;
	}
	
	public static boolean isEquippable(int itemId) {
		return equipment.keySet().contains(itemId);
	}
	
	public static Set<EquipmentTypes> getPlayerEquipmentTypes(int playerId) {
		return getEquippedSlotsAndItemIdsByPlayerId(playerId).keySet().stream()
				.map(e -> getEquipmentTypeByEquipmentId(e))
				.collect(Collectors.toSet());
	}
	
} 