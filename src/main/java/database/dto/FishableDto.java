package database.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter @Getter @AllArgsConstructor
public class FishableDto {
	private int sceneryId;
	private int level;
	private int exp;
	private int itemId;
	private int respawnTicks;
	private int toolId;
	private int baitId;
}
