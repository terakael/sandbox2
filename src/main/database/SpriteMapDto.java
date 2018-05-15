package main.database;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @AllArgsConstructor
public class SpriteMapDto {
	private int id;
	private String name;
	private String dataBase64;
}
