package requests;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AttackRequest extends Request {
	private int objectId;
	private String type; // player, npc, ship
}
