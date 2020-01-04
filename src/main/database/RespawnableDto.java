package main.database;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RespawnableDto {
	private int roomId;
	private int tileId;
	private int itemId;
	private int count;
	private int respawnTicks;
}
