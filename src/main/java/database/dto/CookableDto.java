package database.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CookableDto {
	private final transient int rawItemId;
	private final int cookedItemId;
	private final int level;
	private final transient int exp;
	private final transient int burntItemId;
}
