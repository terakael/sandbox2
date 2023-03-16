package database.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SceneryDto {
	private int id;
	private String name;
	private int spriteFrameId;
	private int leftclickOption;
	private int otherOptions;
	private int impassable;
	private int attributes;
	private int lightsourceRadius;
}
