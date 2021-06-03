package main.database.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StatWindowRowDto {
	public StatWindowRowDto(int level, int itemId) {
		this.level = level;
		this.itemId = itemId;
	}
	
	private int level;
	private int itemId;
	
	// optional fields used for certain stats (e.g. herblore mixes and secondaries, smithing ingredients)
	private Integer itemId2 = null;
	private Integer itemId3 = null;
	private Integer itemId4 = null;
}
