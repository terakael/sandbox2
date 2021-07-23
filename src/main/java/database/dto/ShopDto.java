package database.dto;

import java.util.ArrayList;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ShopDto {
	private int id;
	private int ownerId;
	private String name;
	private int shopType;
	private ArrayList<ShopItemDto> items;
}
