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
	private int count;
	private String friendlyCount;
	private int charges;
}
