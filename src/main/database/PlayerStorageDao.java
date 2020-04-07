package main.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import main.GroundItemManager;
import main.database.entity.update.UpdatePlayerStorageEntity;
import main.processing.DatabaseUpdater;
import main.processing.Player;
import main.processing.WorldProcessor;
import main.types.ItemAttributes;
import main.types.Items;
import main.types.StorageTypes;

public class PlayerStorageDao {
	private static Map<Integer, Map<StorageTypes, Map<Integer, PlayerStorageDto>>> playerStorage; // playerId, <storageTypeId, <slot, dto>>
	
	private PlayerStorageDao() {}
	
	public static List<Integer> getStorageListByPlayerId(int playerId, StorageTypes storageType) {
		if (!validatePlayerStorageElement(playerId, storageType))
			return new ArrayList<>();
		
		return playerStorage.get(playerId).get(storageType).values().stream()
					.map(PlayerStorageDto::getItemId)
					.collect(Collectors.toList());
	}
	
	public static boolean addItemToFirstFreeSlot(int playerId, StorageTypes storageTypeId, int itemId, int count, int charges) {
		List<Integer> invItemIds = getStorageListByPlayerId(playerId, storageTypeId);
		
		boolean existingStackableSlot = false;
		int slot = -1;
		if (ItemDao.itemHasAttribute(itemId, ItemAttributes.STACKABLE) || storageTypeId == StorageTypes.BANK) {
			if (ItemDao.itemHasAttribute(itemId, ItemAttributes.CHARGED)) {
				for (int i = 0; i < invItemIds.size(); ++i) {
					if (invItemIds.get(i) == itemId) {
						InventoryItemDto matchingItem = getStorageItemFromPlayerIdAndSlot(playerId, storageTypeId, i);
						if (matchingItem.getCharges() == charges) {
							slot = i;
							break;
						}
					}
				}
			} else {
				slot = invItemIds.indexOf(itemId);// add to an existing stack if exists
			}
			existingStackableSlot = slot != -1;
		}
		
		if (slot == -1)// no existing stack or not stackable; add to first empty slot
			slot = invItemIds.indexOf(0);
		
		if (slot == -1) {
			// no free slots - drop on the floor lol fuck you
			final Player player = WorldProcessor.getPlayerById(playerId);
			GroundItemManager.add(player.getFloor(), playerId, itemId, player.getTileId(), count, charges);
			return true;
		}
		
		if (existingStackableSlot) {
			addCountToStorageItemSlot(playerId, storageTypeId, slot, count);
		} else {
			return setItemFromPlayerIdAndSlot(playerId, storageTypeId, slot, itemId, count, charges);
		}
		
		return true;
	}
	
	public static Map<Integer, InventoryItemDto> getStorageDtoMapByPlayerId(int playerId, StorageTypes storageType) {
		Map<Integer, InventoryItemDto> dtos = new HashMap<>();
		
		if (!validatePlayerStorageElement(playerId, storageType))
			return dtos;
		
		playerStorage.get(playerId).get(storageType).values()
			.forEach(e -> dtos.put(e.getSlot(), new InventoryItemDto(e)));
		
		return dtos;
	}
	
	public static Map<Integer, InventoryItemDto> getStorageDtoMapByPlayerIdExcludingEmpty(int playerId, StorageTypes storageType) {
		Map<Integer, InventoryItemDto> allDtos = getStorageDtoMapByPlayerId(playerId, storageType);
		Map<Integer, InventoryItemDto> retDtos = new HashMap<>();
		for (Map.Entry<Integer, InventoryItemDto> entry : allDtos.entrySet()) {
			if (entry.getValue().getItemId() > 0)
				retDtos.put(entry.getKey(), entry.getValue());
		}
		return retDtos;
	}
	
	public static Integer getStoredCoalByPlayerId(int playerId) {
		if (!validatePlayerStorageElement(playerId, StorageTypes.FURNACE))
			return 0;
		
		return playerStorage.get(playerId).get(StorageTypes.FURNACE).get(0).getCount();// furnace only has slot 0
	}

	public static ItemDto getItemFromPlayerIdAndSlot(int playerId, int slot) {
		return ItemDao.getItem(getItemIdInSlot(playerId, StorageTypes.INVENTORY, slot));
	}
	
	public static InventoryItemDto getStorageItemFromPlayerIdAndSlot(int playerId, StorageTypes storageType, int slot) {
		if (!validatePlayerStorageElement(playerId, storageType, slot))
			return null;
		
		return new InventoryItemDto(playerStorage.get(playerId).get(storageType).get(slot));
	}
	
	public static Integer getItemIdInSlot(int playerId, StorageTypes storageType, int slot) {
		if (!validatePlayerStorageElement(playerId, storageType, slot))
			return null;
		
		return playerStorage.get(playerId).get(storageType).get(slot).getItemId();
	}

	public static boolean setItemFromPlayerIdAndSlot(int playerId, StorageTypes storageType, int slot, int itemId, int count, int charges) {
		if (!validatePlayerStorageElement(playerId, storageType, slot))
			return false;
		
		playerStorage.get(playerId).get(storageType).get(slot).setItemId(itemId);
		playerStorage.get(playerId).get(storageType).get(slot).setCount(count);
		playerStorage.get(playerId).get(storageType).get(slot).setCharges(charges);
		
		DatabaseUpdater.enqueue(UpdatePlayerStorageEntity.builder()
									.playerId(playerId)
									.storageId(storageType.getValue())
									.slot(slot)
									.itemId(itemId)
									.count(count)
									.charges(charges)
									.build());
		
		return true;
	}
	
	public static int getFreeSlotByPlayerId(int playerId) {
		if (!validatePlayerStorageElement(playerId, StorageTypes.INVENTORY))
			return -1;
		
		Optional<PlayerStorageDto> dto = playerStorage.get(playerId).get(StorageTypes.INVENTORY).values().stream()
				.filter(e -> e.getItemId() == 0)
				.findFirst();
		
		return dto.isPresent() ? dto.get().getSlot() : -1;
	}
	
	public static boolean clearStorageByPlayerIdStorageTypeId(int playerId, StorageTypes storageType) {
		if (!validatePlayerStorageElement(playerId, storageType))
			return false;
		
		playerStorage.get(playerId).get(storageType).values().stream()
		.filter(e -> e.getItemId() != 0)
		.forEach(e -> {
			e.setItemId(0);
			e.setCount(0);
			e.setCharges(0);
			
			// this is a bit slow compared to the single query before, but it's running in a different thread so it's technically faster for the game
			DatabaseUpdater.enqueue(UpdatePlayerStorageEntity.builder()
					.playerId(playerId)
					.storageId(storageType.getValue())
					.slot(e.getSlot())
					.itemId(0)
					.count(0)
					.charges(0)
					.build());
		});
		
		return true;
	}
	
	public static void clearPlayerInventoryExceptFirstThreeSlots(int playerId) {
		if (!validatePlayerStorageElement(playerId, StorageTypes.INVENTORY))
			return;
		
		playerStorage.get(playerId).get(StorageTypes.INVENTORY).values().stream()
			.filter(e -> e.getSlot() >= 3)
			.forEach(e -> {
				e.setCount(0);
				e.setCharges(0);
				e.setItemId(0);
				
				// this is a bit slow compared to the single query before, but it's running in a different thread so it's technically faster for the game
				DatabaseUpdater.enqueue(UpdatePlayerStorageEntity.builder()
						.playerId(playerId)
						.storageId(StorageTypes.INVENTORY.getValue())
						.slot(e.getSlot())
						.itemId(0)
						.count(0)
						.charges(0)
						.build());
			});
	}
	
	// checks the item stack count
	public static int getStorageItemCountByPlayerIdItemIdStorageTypeId(int playerId, int itemId, StorageTypes storageType) {
		if (!validatePlayerStorageElement(playerId, storageType))
			return 0;
		
		// if it's not stackable it will give us the number of inventory slots with this item (as the item count is 1 for each)
		// if it is stackable then it'll give us the count of the item (as there can only be a single inventory slot with the count)
		return playerStorage.get(playerId).get(storageType).values().stream()
					.filter(e -> e.getItemId() == itemId)
					.collect(Collectors.summingInt(PlayerStorageDto::getCount));
	}
	
	public static boolean removeAllItemsFromInventoryByPlayerIdItemId(int playerId, Items item) {
		if (!validatePlayerStorageElement(playerId, StorageTypes.INVENTORY))
			return false;
		
		// pull all the slots from the inventory that contain this itemId
		Set<Integer> slotsWithItemId = playerStorage.get(playerId).get(StorageTypes.INVENTORY).entrySet().stream()
			.filter(e -> e.getValue().getItemId() == item.getValue())
			.map(Map.Entry::getKey)
			.collect(Collectors.toSet());
		
		for (int slot : slotsWithItemId) {
			playerStorage.get(playerId).get(StorageTypes.INVENTORY).get(slot).setItemId(0);
			playerStorage.get(playerId).get(StorageTypes.INVENTORY).get(slot).setCount(0);
			playerStorage.get(playerId).get(StorageTypes.INVENTORY).get(slot).setCharges(0);
			
			DatabaseUpdater.enqueue(UpdatePlayerStorageEntity.builder()
										.playerId(playerId)
										.storageId(StorageTypes.INVENTORY.getValue())
										.slot(slot)
										.itemId(0)
										.count(0)
										.charges(0)
										.build());
		}
		
		return true;
	}

	public static boolean itemExistsInPlayerStorage(int playerId, int itemId) {
		if (!playerStorage.containsKey(playerId))
			return false;
		
		// check inventory, bank, furnace etc
		for (Map<Integer, PlayerStorageDto> dtoMap : playerStorage.get(playerId).values()) {
			for (PlayerStorageDto dto : dtoMap.values()) {
				if (dto.getItemId() == itemId)
					return true;
			}
		}
		
		return false;
	}
	
	public static void addCountToStorageItemSlot(int playerId, StorageTypes storageType, int slot, int count) {
		if (!validatePlayerStorageElement(playerId, storageType, slot))
			return;
		
		int currentCount = playerStorage.get(playerId).get(storageType).get(slot).getCount();
		setCountOnSlot(playerId, storageType, slot, currentCount + count);
	}
	
	public static void setCountOnSlot(int playerId, StorageTypes storageType, int slot, int count) {
		if (!validatePlayerStorageElement(playerId, storageType, slot))
			return;
		
		if (count == 0) {
			// in the case of a stackable, if we are setting the count to zero then remove the item from the inventory (set the item to 0)
			setItemFromPlayerIdAndSlot(playerId, storageType, slot, 0, 1, 0);
			return;
		}
		
		playerStorage.get(playerId).get(storageType).get(slot).setCount(count);
		
		DatabaseUpdater.enqueue(UpdatePlayerStorageEntity.builder()
									.playerId(playerId)
									.storageId(storageType.getValue())
									.slot(slot)
									.count(count)
									.build());
	}

	// TODO this should be done on character creation instead of login?
	public static void createBankSlotsIfNotExists(int playerId) {
		if (!getStorageDtoMapByPlayerId(playerId, StorageTypes.BANK).isEmpty())
			return;
		
		String query = "insert into player_storage values ";
		for (int i = 0; i < 35; ++i) {
			query += String.format("(%d,%d,%d,0,1,0),", playerId, StorageTypes.BANK.getValue(), i);
		}
		query = query.substring(0, query.length() - 1);
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static void cachePlayerStorage() {
		playerStorage = new HashMap<>();
		
		final String query = "select player_id, storage_id, slot, item_id, count, charges from player_storage";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					final int playerId = rs.getInt("player_id");
					final int storageId = rs.getInt("storage_id");
					final StorageTypes storageType = StorageTypes.withValue(storageId);
					
					if (!playerStorage.containsKey(playerId))
						playerStorage.put(playerId, new HashMap<>());
					
					if (!playerStorage.get(playerId).containsKey(storageType))
						playerStorage.get(playerId).put(storageType, new HashMap<>());
					
					PlayerStorageDto dto = new PlayerStorageDto(playerId, storageId, rs.getInt("slot"), rs.getInt("item_id"), rs.getInt("count"), rs.getInt("charges"));
					playerStorage.get(playerId).get(storageType).put(rs.getInt("slot"), dto);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	private static boolean validatePlayerStorageElement(int playerId, StorageTypes storageType, int slot) {
		if (playerStorage == null)
			return false;
		
		if (!playerStorage.containsKey(playerId))
			return false;
		
		if (!playerStorage.get(playerId).containsKey(storageType))
			return false;
		
		if (!playerStorage.get(playerId).get(storageType).containsKey(slot))
			return false;
		
		return true;
	}
	
	private static boolean validatePlayerStorageElement(int playerId, StorageTypes storageType) {
		if (playerStorage == null)
			return false;
		
		if (!playerStorage.containsKey(playerId))
			return false;
		
		if (!playerStorage.get(playerId).containsKey(storageType))
			return false;
		
		return true;
	}
}
