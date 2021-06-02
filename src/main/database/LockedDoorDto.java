package main.database;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class LockedDoorDto {
	private final int floor;
	private final int tileId;
	private final int unlockItemId;
}
