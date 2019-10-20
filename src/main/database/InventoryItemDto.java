package main.database;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import main.types.ItemAttributes;

@Getter
@AllArgsConstructor
public class InventoryItemDto {
	private int itemId;
	private int slot;
	@Setter private int count;
	private String friendlyCount;
	
	public int getStack() {
		return ItemDao.itemHasAttribute(itemId, ItemAttributes.STACKABLE) ? count : 1;
	}
	
	public int getCharges() {
		return ItemDao.itemHasAttribute(itemId,  ItemAttributes.CHARGED) ? count : 1;
	}
}
