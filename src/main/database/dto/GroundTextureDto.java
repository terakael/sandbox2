package main.database.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GroundTextureDto {
	private int id;
//	private int floor;
	private int spriteMapId;
	private int x;
	private int y;
	private boolean walkable;
}
