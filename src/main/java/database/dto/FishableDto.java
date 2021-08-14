package database.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter @Getter @AllArgsConstructor
public class FishableDto {
	private transient int sceneryId;
	private int level;
	private transient int exp;
	private int itemId;
	private transient int respawnTicks;
	private int toolId;
	private int baitId;
}
