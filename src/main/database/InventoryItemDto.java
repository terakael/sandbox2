package main.database;

import lombok.AllArgsConstructor;
import lombok.Getter;
import main.utils.Utils;

@Getter
@AllArgsConstructor
public class InventoryItemDto {
	
	public InventoryItemDto(PlayerStorageDto dto) {
		this.itemId = dto.getItemId();
		this.slot = dto.getSlot();
		this.count = dto.getCount();
		this.friendlyCount = Utils.getFriendlyCount(this.count);
		this.charges = dto.getCharges();
	}
	
	private int itemId;
	private int slot;
	private int count;
	private String friendlyCount;
	private int charges;
}
