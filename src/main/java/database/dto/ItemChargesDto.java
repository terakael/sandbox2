package database.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ItemChargesDto {
	private int itemId;
	private int maxCharges;
	private int degradedItemId;
}
