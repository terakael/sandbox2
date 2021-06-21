package main.database.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ConstructableDto {
	private int resultingSceneryId;
	private int level;
	private int exp;
	private int toolId;
	private int plankId;
	private int plankAmount;
	private int barId;
	private int barAmount;
	private int tertiaryId;
	private int tertiaryAmount;
	private int lifetimeTicks;
}
