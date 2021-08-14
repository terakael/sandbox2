package database.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChoppableDto {
	private int sceneryId;
	private int level;
	private transient int exp;
	private transient int logId;
	private transient int respawnTicks;
}
