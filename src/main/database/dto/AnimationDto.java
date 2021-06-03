package main.database.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class AnimationDto {
	private int id;
	private int spriteMapId;
	
	private int upId;
	private int downId;
	private int leftId;
	private int rightId;
	private int attackLeftId;
	private int attackRightId;
}
