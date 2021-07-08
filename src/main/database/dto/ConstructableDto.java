package main.database.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ConstructableDto {
	private int resultingSceneryId;
	private int level;
	private transient int exp;
	private transient int toolId;
	private int plankId;
	private int plankAmount;
	private int barId;
	private int barAmount;
	private int tertiaryId;
	private int tertiaryAmount;
	private transient int lifetimeTicks;
	private int flatpackItemId;
}
