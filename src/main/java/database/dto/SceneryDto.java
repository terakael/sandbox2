package database.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@NoArgsConstructor
@SuppressWarnings("unused")
public class SceneryDto {
	@Getter private int id;
	@Getter private String name;
	@Getter private int spriteFrameId;
	private int leftclickOption;
	private int otherOptions;
	@Getter private int attributes;
	private int lightsourceRadius;
}
