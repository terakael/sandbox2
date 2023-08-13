package processing.scenery.constructable;

import java.util.ArrayList;
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

public abstract class StorageChest extends Constructable {
	private Map<Integer, Map<Integer, InventoryItemDto>> storage = new HashMap<>(); // playerId, <slot, item>

	public StorageChest(int playerId, int floor, int tileId, int lifetimeTicks, ConstructableDto dto, boolean onHousingTile, ResponseMaps responseMaps) {
		super(playerId, floor, tileId, lifetimeTicks, dto, onHousingTile, responseMaps);
		
		if (onHousingTile) {
			// TODO this won't scale well, need to pre-cache all persisted storage chest items
			final String query = "select player_id, slot, item_id, count, charges from housing_constructable_storage where floor=? and tile_id=?";
			DbConnection.load(query, rs -> {
				storage.putIfAbsent(rs.getInt("player_id"), new HashMap<>());
				storage.get(rs.getInt("player_id")).put(rs.getInt("slot"), 
						new InventoryItemDto(rs.getInt("item_id"), rs.getInt("slot"), rs.getInt("count"), rs.getInt("charges")));
			}, floor, tileId);
		}
	}
	
	@Override
	public void onDestroy(ResponseMaps responseMaps) {
		storage.forEach((playerId, storageMap) -> {
			Player player = WorldProcessor.getPlayerById(playerId);
			storageMap.values().forEach(item -> {
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
			Map<Integer, InventoryItemDto> map = new HashMap<>();
			for (int i = 0; i < getMaxSlots(); ++i) {
				map.put(i, new InventoryItemDto(0, i, 0, 0));
			}
			storage.put(playerId, map);
			
			if (onHousingTile) {
				for (int i = 0; i < getMaxSlots(); ++i)
					DatabaseUpdater.enqueue(new InsertHousingConstructableStorageEntity(floor, tileId, playerId, dto.getResultingSceneryId(), i, 0, 0, 0));
			}
		}
	}
	
	public void addStackable(int playerId, InventoryItemDto itemDto, int count) {
		if (!storage.containsKey(playerId))
			open(playerId);
		
		for (Map.Entry<Integer, InventoryItemDto> slot : storage.get(playerId).entrySet()) {
			if (slot.getValue().getItemId() == itemDto.getItemId() && slot.getValue().getCharges() == itemDto.getCharges()) {
				if (slot.getValue().getCount() + count <= 0) {
					// we've withdrawn everything, so remove the item
					remove(playerId, slot.getKey());
				} else {
					slot.getValue().addCount(count);
					if (onHousingTile)
						DatabaseUpdater.enqueue(UpdateHousingConstructableStorageEntity.builder()
								.floor(floor)
								.tileId(tileId)
								.playerId(playerId)
								.slot(slot.getKey())
								.count(slot.getValue().getCount())
								.build());
				}
				return;
			}
		}
		
		// if for some reason we're trying to withdraw something that doesn't exist, we shouldn't be adding anything here.
		if (count > 0) {
			addInternal(playerId, itemDto, count);
//			for (Map.Entry<Integer, InventoryItemDto> slot : storage.get(playerId).entrySet()) {
//				if (slot.getValue().getItemId() == 0) {
//					slot.setValue(new InventoryItemDto(itemDto.getItemId(), slot.getKey(), count, itemDto.getCharges()));
//					return;
//				}
//			}
		}
	}
	
	private void addInternal(int playerId, InventoryItemDto itemDto, int count) {
		for (Map.Entry<Integer, InventoryItemDto> slot : storage.get(playerId).entrySet()) {
			if (slot.getValue().getItemId() == 0) {
				slot.setValue(new InventoryItemDto(itemDto.getItemId(), slot.getKey(), count, itemDto.getCharges()));
				if (onHousingTile)
					DatabaseUpdater.enqueue(UpdateHousingConstructableStorageEntity.builder()
							.floor(floor)
							.tileId(tileId)
							.playerId(playerId)
							.slot(slot.getKey())
							.itemId(itemDto.getItemId())
							.count(count)
							.charges(itemDto.getCharges())
							.build());
				return;
			}
		}
	}
	
	public void add(int playerId, InventoryItemDto itemDto) {
		if (!storage.containsKey(playerId))
			open(playerId);
		
		addInternal(playerId, itemDto, itemDto.getCount());
		
//		for (Map.Entry<Integer, InventoryItemDto> slot : storage.get(playerId).entrySet()) {
//			if (slot.getValue().getItemId() == 0) {
//				slot.setValue(new InventoryItemDto(itemDto.getItemId(), slot.getKey(), itemDto.getCount(), itemDto.getCharges()));
//				return;
//			}
//		}
	}
	
	public void remove(int playerId, int slot) {
		if (!storage.containsKey(playerId))
			return;
		storage.get(playerId).put(slot, new InventoryItemDto(0, slot, 0, 0));
		
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
	}
	
	public void swapSlotContents(int playerId, int src, int dest) {
		if (!storage.containsKey(playerId))
			return;
		
		InventoryItemDto srcSlot = storage.get(playerId).get(src);
		InventoryItemDto destSlot = storage.get(playerId).get(dest);
		
		storage.get(playerId).put(src, new InventoryItemDto(destSlot.getItemId(), srcSlot.getSlot(), destSlot.getCount(), destSlot.getCharges()));
		storage.get(playerId).put(dest,  new InventoryItemDto(srcSlot.getItemId(), destSlot.getSlot(), srcSlot.getCount(), srcSlot.getCharges()));
		
		if (onHousingTile) {
			DatabaseUpdater.enqueue(UpdateHousingConstructableStorageEntity.builder()
					.floor(floor)
					.tileId(tileId)
					.playerId(playerId)
					.slot(src)
					.itemId(destSlot.getItemId())
					.count(destSlot.getCount())
					.charges(destSlot.getCharges())
					.build());
			
			DatabaseUpdater.enqueue(UpdateHousingConstructableStorageEntity.builder()
					.floor(floor)
					.tileId(tileId)
					.playerId(playerId)
					.slot(dest)
					.itemId(srcSlot.getItemId())
					.count(srcSlot.getCount())
					.charges(srcSlot.getCharges())
					.build());
		}
	}
	
	public boolean contains(int playerId, int itemId) {
		if (!storage.containsKey(playerId))
			return false;
		
		return storage.get(playerId).entrySet().stream()
				.filter(entry -> entry.getValue().getItemId() == itemId)
				.count() > 0;
	}
	
	public int getEmptySlotCount(int playerId) {
		if (!storage.containsKey(playerId))
			return getMaxSlots();
		return (int)storage.get(playerId).values().stream().filter(e -> e.getItemId() == 0).count();
	}

	public abstract int getMaxSlots();
	public abstract String getName(); // to display on the UI
	
	public List<InventoryItemDto> getItems(int playerId) {
		if (!storage.containsKey(playerId))
			open(playerId);
		return new ArrayList<>(storage.get(playerId).values());
	}
}
