package main.database.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ConsumableEffectsDto {
	private int itemId;
	private int statId;
	private int amount;
	private int pct;
}
