package main.database;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InventoryItemDto {
	private int itemId;
	private int slot;
	private int count;
}
