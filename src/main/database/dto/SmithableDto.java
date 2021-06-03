package main.database.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SmithableDto {
	private int itemId;
	private String itemName;
	private int level;
	private int material1;
	private String material1Name;
	private int count1;
	private int material2;
	private String material2Name;
	private int count2;
	private int material3;
	private String material3Name;
	private int count3;
}
