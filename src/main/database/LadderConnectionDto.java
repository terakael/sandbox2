package main.database;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LadderConnectionDto {
	private int fromRoomId;
	private int fromTileId;
	private int toRoomId;
	private int toTileId;
}
