package main.database;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ConsumableDto {
	private int itemId;
	private int statId;
	private int amount;
}
