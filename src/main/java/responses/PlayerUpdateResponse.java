package responses;

import java.util.Map;

import database.dto.PlayerAnimationDto;
import lombok.Getter;
import lombok.Setter;
import processing.attackable.Player;
import requests.Request;
import types.DamageTypes;
import types.EquipmentTypes;
import types.PlayerPartType;

@SuppressWarnings("unused")
public class PlayerUpdateResponse extends Response {
	@Getter @Setter private Integer id = null;
	@Setter private String name = null;
	@Setter private Integer combatLevel = null;
	@Setter private Integer tileId = null;
	@Setter private Integer currentHp = null;
	@Setter private Integer currentPrayer = null;
	@Setter private Integer maxHp = null;
	@Setter private Integer relativeFloor = null;
	@Setter private Boolean loggedIn = null;
	@Setter private Boolean snapToTile = null;
	@Setter private Boolean respawn = null;
	@Setter private String faceDirection = null;
	@Setter private Boolean doAttack = null;
	@Setter private Map<PlayerPartType, PlayerAnimationDto> baseAnimations = null;
	@Setter private Map<PlayerPartType, PlayerAnimationDto> equipAnimations = null;
	@Setter private EquipmentTypes weaponType = null;
	private Integer damage = null;
	private Integer damageSpriteFrameId = null;

	public PlayerUpdateResponse() {
		setAction("player_update");
	}
	
	@Override
	protected boolean handleCombat(Request req, Player player, ResponseMaps responseMaps) {
		return true;
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {

	}
	
	public void setDamage(int damage, DamageTypes type) {
		this.damage = damage;
		
		switch (type) {
		case STANDARD:
			damageSpriteFrameId = damage == 0 ? 1025 : 1024;
			break;
		case POISON:
			damageSpriteFrameId = 1026;
			break;
		case MAGIC:
			damageSpriteFrameId = 1027;
			break;
		}
	}

}
