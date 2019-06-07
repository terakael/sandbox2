package main.database;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PlayerDto {
	private int id;
	private String name;
	private transient String password;
	private int tileId;
	private int currentHp;
	private int maxHp;
	private int combatLevel;
	private int attackStyleId;
	private AnimationDto animations;
}
