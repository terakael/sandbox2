package database.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RespawnableDto {
	private int floor;
	private int tileId;
	private int itemId;
	private int count;
	private int respawnTicks;
}
