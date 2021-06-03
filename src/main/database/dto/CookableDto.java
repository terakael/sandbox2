package main.database.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class CookableDto {
	int rawItemId;
	int cookedItemId;
	int level;
	int exp;
	int burntItemId;
}
