package database.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TeleportableDto {
	private int itemId;
	private int floor;
	private int tileId;
}
