package database.dto;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import types.EquipmentTypes;
import types.PlayerPartType;

@Data
@AllArgsConstructor
public class PlayerDto {
	// TODO deprecate in favour of PlayerUpdateResponse
	// i.e. on login, send a playerUpdateResponse for the client player
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
	private EquipmentTypes weaponType = null;
}
