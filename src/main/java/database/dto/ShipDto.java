package database.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ShipDto {
	private final int hullSceneryId;
	private final String name;
	transient private final int slotSize;
	private final int upId;
	private final int downId;
	private final int leftId;
	private final int rightId;
	private final int leftclickOption;
	private final int otherOptions;
}
