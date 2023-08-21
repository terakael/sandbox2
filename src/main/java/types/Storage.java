package types;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import database.dto.InventoryItemDto;
import lombok.Setter;

public class Storage {
	private final Map<Integer, InventoryItemDto> _storage;
	@Setter private Consumer<InventoryItemDto> addStackableCallback = null;
	@Setter private Consumer<InventoryItemDto> addCallback = null;
	@Setter private Consumer<Integer> removeCallback = null;
	@Setter private BiConsumer<InventoryItemDto, InventoryItemDto> swapSlotCallback = null;
	
	public Storage(int slots) {
		_storage = new HashMap<>();
		for (int i = 0; i < slots; ++i)
			_storage.put(i, new InventoryItemDto(0, i, 0, 0));
	}
	
	public void addStackable(InventoryItemDto itemDto, int count) {
		for (Map.Entry<Integer, InventoryItemDto> slot : _storage.entrySet()) {
			if (slot.getValue().getItemId() == itemDto.getItemId() && slot.getValue().getCharges() == itemDto.getCharges()) {
				if (slot.getValue().getCount() + count <= 0) {
					// we've withdrawn everything, so remove the item
					remove(slot.getKey());
				} else {
					slot.getValue().addCount(count);
					
					if (addStackableCallback != null)
						addStackableCallback.accept(slot.getValue());
				}
				return;
			}
		}
		
		// there's no existing stackable object - add it as a new entry
		if (count > 0) {
			add(itemDto, count);
		}
	}
	
	public void add(InventoryItemDto itemDto, int count) {
		for (Map.Entry<Integer, InventoryItemDto> slot : _storage.entrySet()) {
			if (slot.getValue().getItemId() == 0) {
				slot.setValue(new InventoryItemDto(itemDto.getItemId(), slot.getKey(), count, itemDto.getCharges()));
				
				if (addCallback != null)
					addCallback.accept(slot.getValue());
				return;
			}
		}
	}
	
	public void remove(int slot) {
		_storage.put(slot, new InventoryItemDto(0, slot, 0, 0));
		
		if (removeCallback != null)
			removeCallback.accept(slot);
	}
	
	public void swapSlotContents(int src, int dest) {
		InventoryItemDto srcSlot = _storage.get(src);
		InventoryItemDto destSlot = _storage.get(dest);
		
		if (srcSlot == null || destSlot == null)
			return;
		
		_storage.put(src, new InventoryItemDto(destSlot.getItemId(), srcSlot.getSlot(), destSlot.getCount(), destSlot.getCharges()));
		_storage.put(dest, new InventoryItemDto(srcSlot.getItemId(), destSlot.getSlot(), srcSlot.getCount(), srcSlot.getCharges()));
		
		if (swapSlotCallback != null)
			swapSlotCallback.accept(_storage.get(src), _storage.get(dest));
	}
	
	public boolean contains(int itemId) {
		return getItemById(itemId) != null;
	}
	
	public boolean contains(Items itemId) {
		return contains(itemId.getValue());
	}
	
	public InventoryItemDto getFirstItemOf(Items... itemIds) {
		return _storage.values().stream()
				.filter(e -> Arrays.stream(itemIds).anyMatch(f -> f.getValue() == e.getItemId()))
				.min((i1, i2) -> Integer.compare(i1.getSlot(), i2.getSlot()))
				.orElse(null);
	}
	
	public int getEmptySlotCount(int playerId) {
		return (int)_storage.values().stream().filter(e -> e.getItemId() == 0).count();
	}
	
	public List<InventoryItemDto> getItems() {
		return new ArrayList<>(_storage.values());
	}
	
	public InventoryItemDto getItemById(int itemId) {
		return _storage.values().stream()
				.filter(item -> item.getItemId() == itemId)
				.findFirst()
				.orElse(null);
	}
}
