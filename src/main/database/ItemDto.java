package main.database;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @AllArgsConstructor
public class ItemDto {
	private int id;
	private String name;
	private int spriteFrameId;
	private int leftclickOption;
	private int otherOptions;
	private int attributes;
	private int price;
}
