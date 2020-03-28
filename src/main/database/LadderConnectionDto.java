package main.database;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LadderConnectionDto {
	private int fromFloor;
	private int fromTileId;
	private int toFloor;
	private int toTileId;
}
