package main.database;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter @Getter @AllArgsConstructor
public class MineableDto {
	private int sceneryId;
	private int level;
	private int exp;
	private int itemId;
	private int respawnTicks;
}
