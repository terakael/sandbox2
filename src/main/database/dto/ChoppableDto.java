package main.database.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChoppableDto {
	private int sceneryId;
	private int level;
	private int exp;
	private int logId;
	private int respawnTicks;
}
