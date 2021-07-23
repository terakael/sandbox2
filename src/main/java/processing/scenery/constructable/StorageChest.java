package processing.scenery.constructable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import database.dto.ConstructableDto;
import database.dto.InventoryItemDto;
import processing.WorldProcessor;
import processing.attackable.Player;
import responses.ResponseMaps;
import system.GroundItemManager;

public abstract class StorageChest extends Constructable {
	private Map<Integer, Map<Integer, InventoryItemDto>> storage = new HashMap<>(); // playerId, <slot, item>

	public StorageChest(int floor, int tileId, ConstructableDto dto) {
		super(floor, tileId, dto);
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
		});
	}
	
	public void open(int playerId) {
		if (!storage.containsKey(playerId)) {
			Map<Integer, InventoryItemDto> map = new HashMap<>();
			for (int i = 0; i < getMaxSlots(); ++i) {
				map.put(i, new InventoryItemDto(0, i, 0, 0));
			}
			storage.put(playerId, map);
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
				}
				return;
			}
		}
		
		// if for some reason we're trying to withdraw something that doesn't exist, we shouldn't be adding anything here.
		if (count > 0) {
			for (Map.Entry<Integer, InventoryItemDto> slot : storage.get(playerId).entrySet()) {
				if (slot.getValue().getItemId() == 0) {
					slot.setValue(new InventoryItemDto(itemDto.getItemId(), slot.getKey(), count, itemDto.getCharges()));
					return;
				}
			}
		}
	}
	
	public void add(int playerId, InventoryItemDto itemDto) {
		if (!storage.containsKey(playerId))
			open(playerId);
		
		for (Map.Entry<Integer, InventoryItemDto> slot : storage.get(playerId).entrySet()) {
			if (slot.getValue().getItemId() == 0) {
				slot.setValue(new InventoryItemDto(itemDto.getItemId(), slot.getKey(), itemDto.getCount(), itemDto.getCharges()));
				return;
			}
		}
	}
	
	public void remove(int playerId, int slot) {
		if (!storage.containsKey(playerId))
			return;
		storage.get(playerId).put(slot, new InventoryItemDto(0, slot, 0, 0));
	}
	
	public void swapSlotContents(int playerId, int src, int dest) {
		if (!storage.containsKey(playerId))
			return;
		
		InventoryItemDto srcSlot = storage.get(playerId).get(src);
		InventoryItemDto destSlot = storage.get(playerId).get(dest);
		
		storage.get(playerId).put(src, new InventoryItemDto(destSlot.getItemId(), srcSlot.getSlot(), destSlot.getCount(), destSlot.getCharges()));
		storage.get(playerId).put(dest,  new InventoryItemDto(srcSlot.getItemId(), destSlot.getSlot(), srcSlot.getCount(), srcSlot.getCharges()));
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
