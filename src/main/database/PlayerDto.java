package main.database;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PlayerDto {
	private int id;
	private String name;
	private transient String password;
	private int x;
	private int y;
	private int currentHp;
	private int maxHp;
	private AnimationDto animations;
}
