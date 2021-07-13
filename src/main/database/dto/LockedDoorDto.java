package main.database.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class LockedDoorDto {
	private final int floor;
	private final int tileId;
	private final int unlockItemId;
	private final boolean destroyOnUse;
}
