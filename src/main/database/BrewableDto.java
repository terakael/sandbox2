package main.database;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class BrewableDto {
	private int potionId;
	private int level;
	private int exp;
}
