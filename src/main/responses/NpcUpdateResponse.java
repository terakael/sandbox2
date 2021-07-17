package main.responses;

import lombok.Setter;
import main.processing.Player;
import main.requests.Request;
import main.types.DamageTypes;

@SuppressWarnings("unused")
public class NpcUpdateResponse extends Response {
	@Setter private Integer instanceId = null;
	private Integer damage = null;
	private Integer damageSpriteFrameId = null;
	@Setter private Integer hp = null;
	@Setter private Integer tileId = null;
	@Setter private Boolean snapToTile = null;
	@Setter private Boolean doAttack = null;
	
	public NpcUpdateResponse() {
		setAction("npc_update");
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
