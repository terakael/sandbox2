package database.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class MineableDto {
	private final int sceneryId;
	private final int level;
	private final int exp;
	private final int itemId;
	private final int respawnTicks;
	private final int goldChance;
}
