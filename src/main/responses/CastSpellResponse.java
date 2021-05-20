package main.responses;

import main.processing.Player;
import main.requests.Request;

@SuppressWarnings("unused")
public class CastSpellResponse extends Response {
	private int playerId;
	private int targetId;
	private String targetType = null;
	private int spriteFrameId;
	
	public CastSpellResponse(int playerId, int targetId, String targetType, int spriteFrameId) {
		setAction("cast_spell");
		this.playerId = playerId;
		this.targetId = targetId;
		this.targetType = targetType;
		this.spriteFrameId = spriteFrameId;
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		
	}

}
