package database.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ConstructableDto {
	private final int resultingSceneryId;
	private final int level;
	private final transient int exp;
	private final transient int toolId;
	private final int plankId;
	private final int plankAmount;
	private final int barId;
	private final int barAmount;
	private final int tertiaryId;
	private final int tertiaryAmount;
	private final transient int lifetimeTicks;
	private final int flatpackItemId;
	private final int landType; // bitmap: 1=land, 2=water, 4=lava?
}
