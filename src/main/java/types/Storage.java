package types;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import database.dto.InventoryItemDto;
import lombok.Getter;
import lombok.Setter;

public class Storage {
	private final List<InventoryItemDto> _storage;
	@Setter @Getter private boolean allItemsStackable = false;
	@Setter private Consumer<InventoryItemDto> addCallback = null;
	
	public Storage(int slots) {
		_storage = new LinkedList<>();
		for (int i = 0; i < slots; ++i)
			_storage.add(new InventoryItemDto(0, i, 0, 0));
	}
	
	public void addStackable(InventoryItemDto itemDto) {
		addStackable(itemDto, itemDto.getCount());
	}
	
	public void addStackable(InventoryItemDto itemDto, int count) {
		for (InventoryItemDto slot : _storage) {
			if (slot.getItemId() == itemDto.getItemId() && slot.getCharges() == itemDto.getCharges()) {
				final int newCount = slot.getCount() + count;
				if (newCount <= 0) {
					// we've withdrawn everything, so remove the item
					remove(slot.getSlot());
				} else {
					set(itemDto.getItemId(), slot.getSlot(), newCount, slot.getCharges());
				}
				return;
			}
		}
		
		// there's no existing stackable object - add it as a new entry
		if (count > 0) {
			add(itemDto.getItemId(), count, itemDto.getCharges());
		}
	}
	
	public void add(InventoryItemDto itemDto, int count) {
		add(itemDto.getItemId(), count, itemDto.getCharges());
	}
	
	public void add(int itemId, int count, int charges) {
		final int slot = _storage.stream()
				.filter(e -> e.getItemId() == 0)
				.map(InventoryItemDto::getSlot)
				.findFirst()
				.orElse(-1);
		
		if (slot == -1)
			return; // no free slots
		
		set(itemId, slot, count, charges);
	}
	
	public void set(int itemId, int slot, int count, int charges) {
		final InventoryItemDto item = _storage.get(slot);
		item.setItemId(itemId);
		item.setCount(count);
		item.setCharges(charges);
		
		if (addCallback != null)
			addCallback.accept(item);
	}
	
	public void remove(int slot) {
		set(0, slot, 0, 0);
	}
	
	public void swapSlotContents(int src, int dest) {
		final InventoryItemDto srcSlot = new InventoryItemDto(_storage.get(src));
		final InventoryItemDto destSlot = new InventoryItemDto(_storage.get(dest));
		
		if (srcSlot == null || destSlot == null)
			return;
		
		set(destSlot.getItemId(), srcSlot.getSlot(), destSlot.getCount(), destSlot.getCharges());
		set(srcSlot.getItemId(), destSlot.getSlot(), srcSlot.getCount(), srcSlot.getCharges());
	}
	
	public boolean contains(int itemId) {
		return getItemById(itemId) != null;
	}
	
	public boolean contains(Items itemId) {
		return contains(itemId.getValue());
	}
	
	public InventoryItemDto getFirstItemOf(Items... itemIds) {
		return _storage.stream()
				.filter(e -> Arrays.stream(itemIds).anyMatch(f -> f.getValue() == e.getItemId()))
				.min((i1, i2) -> Integer.compare(i1.getSlot(), i2.getSlot()))
				.orElse(null);
	}
	
	public int getEmptySlotCount() {
		return (int)_storage.stream().filter(e -> e.getItemId() == 0).count();
	}
	
	public List<InventoryItemDto> getItems() {
		return _storage;
	}
	
	public InventoryItemDto getItemById(int itemId) {
		return _storage.stream()
				.filter(item -> item.getItemId() == itemId)
				.findFirst()
				.orElse(null);
	}
}
