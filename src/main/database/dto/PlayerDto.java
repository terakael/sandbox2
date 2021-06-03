package main.database.dto;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import main.types.PlayerPartType;

@Data
@AllArgsConstructor
public class PlayerDto {
	private int id;
	private String name;
	private transient String password;
	private int tileId;
	private int floor;
	private int currentHp;
	private int maxHp;
	private int currentPrayer;
	private int combatLevel;
	private int attackStyleId;
	private Map<PlayerPartType, PlayerAnimationDto> baseAnimations;
	private Map<PlayerPartType, PlayerAnimationDto> equipAnimations;
}
