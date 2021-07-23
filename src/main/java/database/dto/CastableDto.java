package database.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CastableDto {
	private int itemId;
	private int level;
	private int exp;
	private int maxHit;
	private int spriteFrameId;
}
