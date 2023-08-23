package database.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class InventoryItemDto {
	
	public InventoryItemDto(PlayerStorageDto dto) {
		this.itemId = dto.getItemId();
		this.slot = dto.getSlot();
		this.count = dto.getCount();
		this.charges = dto.getCharges();
	}
	
	public InventoryItemDto(InventoryItemDto dto) {
		this.itemId = dto.getItemId();
		this.slot = dto.getSlot();
		this.count = dto.getCount();
		this.charges = dto.getCharges();
	}
	
	private int itemId;
	private int slot;
	private int count;
	private int charges;
	
//	public void addCount(int newCount) {
//		count += newCount;
//	}
}
