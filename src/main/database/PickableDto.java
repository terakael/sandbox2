package main.database;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PickableDto {
	private int sceneryId;
	private int itemId;
	private int respawnTicks;
}
