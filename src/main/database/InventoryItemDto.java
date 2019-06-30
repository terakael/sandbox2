package main.database;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@AllArgsConstructor
public class InventoryItemDto {
	private int itemId;
	private int slot;
	@Setter private int count;
	private String friendlyCount;
}
