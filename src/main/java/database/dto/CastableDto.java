package database.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CastableDto {
	private final int itemId;
	private final int level;
	private final transient int exp;
	private final transient int maxHit;
	private final transient int spriteFrameId;
}
