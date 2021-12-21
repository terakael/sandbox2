package database.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class MineableDto {
	private final int sceneryId;
	private final int level;
	private transient final int exp;
	private transient final int itemId;
	private transient final int respawnTicks;
	private transient final int goldChance;
}
