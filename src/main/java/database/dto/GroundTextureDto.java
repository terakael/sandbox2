package database.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GroundTextureDto {
	private int id;
	private String dataBase64;
	private transient boolean walkable;
	
//	private int id;
//	private int spriteMapId;
//	private int x;
//	private int y;
//	private boolean walkable;
}
