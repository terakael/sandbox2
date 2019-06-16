package main.database;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@AllArgsConstructor
public class ShopDto {
	private int itemId;
	@Setter private int currentStock;
	private int maxStock;
	private int price;
}
