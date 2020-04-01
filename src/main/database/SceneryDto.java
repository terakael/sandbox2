package main.database;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@NoArgsConstructor
public class SceneryDto {
	@Getter private int id;
	private String name;
	private int spriteFrameId;
	private int leftclickOption;
	private int otherOptions;
	private int attributes;
}
