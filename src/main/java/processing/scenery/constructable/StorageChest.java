package processing.scenery.constructable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import database.DbConnection;
import database.dto.ConstructableDto;
import database.dto.InventoryItemDto;
import database.entity.delete.DeleteHousingConstructableStorageEntity;
import database.entity.insert.InsertHousingConstructableStorageEntity;
import database.entity.update.UpdateHousingConstructableStorageEntity;
import processing.WorldProcessor;
import processing.attackable.Player;
import processing.managers.DatabaseUpdater;
import responses.ResponseMaps;
import system.GroundItemManager;
import types.Storage;

public abstract class StorageChest extends Constructable {
	private Map<Integer, Storage> storage = new HashMap<>(); // playerId, <slot, item>

	public StorageChest(int playerId, int floor, int tileId, int lifetimeTicks, ConstructableDto dto, boolean onHousingTile, ResponseMaps responseMaps) {
		super(playerId, floor, tileId, lifetimeTicks, dto, onHousingTile, responseMaps);
		
		if (onHousingTile) {
			// TODO this won't scale well, need to pre-cache all persisted storage chest items
			final String query = "select player_id, slot, item_id, count, charges from housing_constructable_storage where floor=? and tile_id=? order by player_id, slot";
			DbConnection.load(query, rs -> {
				storage.putIfAbsent(rs.getInt("player_id"), new Storage(getMaxSlots()));				
				storage.get(rs.getInt("player_id")).add(
						new InventoryItemDto(rs.getInt("item_id"), rs.getInt("slot"), rs.getInt("count"), rs.getInt("charges")), rs.getInt("count"));
				addCallbacks(rs.getInt("player_id"), storage.get(rs.getInt("player_id")));
			}, floor, tileId);
		}
	}
	
	public Storage getPlayerStorage(int playerId) {
		return storage.get(playerId);
	}
	
	private void addCallbacks(int playerId, Storage storage) {
		storage.setAddStackableCallback(item -> {
			if (onHousingTile) {
				DatabaseUpdater.enqueue(UpdateHousingConstructableStorageEntity.builder()
					.floor(floor)
					.tileId(tileId)
					.playerId(playerId)
					.slot(item.getSlot())
					.count(item.getCount())
					.build());
			}
		});
		
		storage.setAddCallback(item -> {
			if (onHousingTile)
				DatabaseUpdater.enqueue(UpdateHousingConstructableStorageEntity.builder()
					.floor(floor)
					.tileId(tileId)
					.playerId(playerId)
					.slot(item.getSlot())
					.itemId(item.getItemId())
					.count(item.getCount())
					.charges(item.getCharges())
					.build());
		});
		
		storage.setRemoveCallback(slot -> {
			if (onHousingTile)
				DatabaseUpdater.enqueue(UpdateHousingConstructableStorageEntity.builder()
					.floor(floor)
					.tileId(tileId)
					.playerId(playerId)
					.slot(slot)
					.itemId(0)
					.count(0)
					.charges(0)
					.build());
		});
		
		storage.setSwapSlotCallback((newSrcItem, newDestItem) -> {
			if (onHousingTile) {
				DatabaseUpdater.enqueue(UpdateHousingConstructableStorageEntity.builder()
						.floor(floor)
						.tileId(tileId)
						.playerId(playerId)
						.slot(newSrcItem.getSlot())
						.itemId(newSrcItem.getItemId())
						.count(newSrcItem.getCount())
						.charges(newSrcItem.getCharges())
						.build());
				
				DatabaseUpdater.enqueue(UpdateHousingConstructableStorageEntity.builder()
						.floor(floor)
						.tileId(tileId)
						.playerId(playerId)
						.slot(newDestItem.getSlot())
						.itemId(newDestItem.getItemId())
						.count(newDestItem.getCount())
						.charges(newDestItem.getCharges())
						.build());
		}
		});
	}
	
	@Override
	public void onDestroy(ResponseMaps responseMaps) {
		storage.forEach((playerId, playerStorage) -> {
			Player player = WorldProcessor.getPlayerById(playerId);
			playerStorage.getItems().forEach(item -> {
				if (player != null) {
					GroundItemManager.add(floor, playerId, item.getItemId(), tileId, item.getCount(), item.getCharges());
				} else {
					GroundItemManager.addGlobally(floor, tileId, item.getItemId(), item.getCount(), item.getCharges());
				}
			});
			
			if (onHousingTile) {
				for (int i = 0; i < getMaxSlots(); ++i)
					DatabaseUpdater.enqueue(DeleteHousingConstructableStorageEntity.builder()
							.floor(floor)
							.tileId(tileId)
							.playerId(playerId)
							.slot(i)
							.build());
			}
		});
	}
	
	public void open(int playerId) {
		if (!storage.containsKey(playerId)) {
			storage.put(playerId, new Storage(getMaxSlots()));
			addCallbacks(playerId, storage.get(playerId));
			
			for (int i = 0; i < getMaxSlots(); ++i) {
				if (onHousingTile)
					DatabaseUpdater.enqueue(new InsertHousingConstructableStorageEntity(floor, tileId, playerId, dto.getResultingSceneryId(), i, 0, 0, 0));
			}
		}
	}
	
	public void addStackable(int playerId, InventoryItemDto itemDto, int count) {
		if (!storage.containsKey(playerId))
			open(playerId);
		
		storage.get(playerId).addStackable(itemDto, count);
	}
	
//	private void addInternal(int playerId, InventoryItemDto itemDto, int count) {
//		for (Map.Entry<Integer, InventoryItemDto> slot : storage.get(playerId).entrySet()) {
//			if (slot.getValue().getItemId() == 0) {
//				slot.setValue(new InventoryItemDto(itemDto.getItemId(), slot.getKey(), count, itemDto.getCharges()));
//				if (onHousingTile)
//					DatabaseUpdater.enqueue(UpdateHousingConstructableStorageEntity.builder()
//							.floor(floor)
//							.tileId(tileId)
//							.playerId(playerId)
//							.slot(slot.getKey())
//							.itemId(itemDto.getItemId())
//							.count(count)
//							.charges(itemDto.getCharges())
//							.build());
//				return;
//			}
//		}
//	}
	
	public void add(int playerId, InventoryItemDto itemDto) {
		if (!storage.containsKey(playerId))
			open(playerId);
		
		storage.get(playerId).add(itemDto, itemDto.getCount());
		
		// TODO persist the storage into the database when its on a housing tile
//		if (onHousingTile) {
//			DatabaseUpdater.enqueue(UpdateHousingConstructableStorageEntity.builder()
//					.floor(floor)
//					.tileId(tileId)
//					.playerId(playerId)
//					.slot(slot.getKey())
//					.itemId(itemDto.getItemId())
//					.count(count)
//					.charges(itemDto.getCharges())
//					.build());
//		}
	}
	
	public void remove(int playerId, int slot) {
		if (!storage.containsKey(playerId))
			return;
		storage.get(playerId).remove(slot);
	}
	
	public void swapSlotContents(int playerId, int src, int dest) {
		if (!storage.containsKey(playerId))
			return;
		storage.get(playerId).swapSlotContents(src, dest);
		
//		InventoryItemDto srcSlot = storage.get(playerId).get(src); 
//		InventoryItemDto destSlot = storage.get(playerId).get(dest);
//		
//		storage.get(playerId).put(src, new InventoryItemDto(destSlot.getItemId(), srcSlot.getSlot(), destSlot.getCount(), destSlot.getCharges()));
//		storage.get(playerId).put(dest,  new InventoryItemDto(srcSlot.getItemId(), destSlot.getSlot(), srcSlot.getCount(), srcSlot.getCharges()));
		
//		if (onHousingTile) {
//			DatabaseUpdater.enqueue(UpdateHousingConstructableStorageEntity.builder()
//					.floor(floor)
//					.tileId(tileId)
//					.playerId(playerId)
//					.slot(src)
//					.itemId(destSlot.getItemId())
//					.count(destSlot.getCount())
//					.charges(destSlot.getCharges())
//					.build());
//			
//			DatabaseUpdater.enqueue(UpdateHousingConstructableStorageEntity.builder()
//					.floor(floor)
//					.tileId(tileId)
//					.playerId(playerId)
//					.slot(dest)
//					.itemId(srcSlot.getItemId())
//					.count(srcSlot.getCount())
//					.charges(srcSlot.getCharges())
//					.build());
//		}
	}
	
	public boolean contains(int playerId, int itemId) {
		if (!storage.containsKey(playerId))
			return false;
		
		return storage.get(playerId).contains(itemId);
	}
	
	public int getEmptySlotCount(int playerId) {
		if (!storage.containsKey(playerId))
			return getMaxSlots();
		return storage.get(playerId).getEmptySlotCount(playerId);
	}

	public abstract int getMaxSlots();
	public abstract String getName(); // to display on the UI
	
	public List<InventoryItemDto> getItems(int playerId) {
		if (!storage.containsKey(playerId))
			open(playerId);
		return storage.get(playerId).getItems();
	}
}
