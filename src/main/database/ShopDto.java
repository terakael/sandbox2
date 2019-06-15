package main.database;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ShopDto {
	private int itemId;
	private int currentStock;
	private int maxStock;
	private int slot;
	private int price;
}
