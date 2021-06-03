package main.database.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class DoorDto {
	private int sceneryId;
	private int openImpassable;
	private int closedImpassable;
}
