package main.responses;

import main.database.entity.update.UpdatePlayerEntity;
import main.processing.DatabaseUpdater;
import main.processing.Player;
import main.requests.Request;

public class ToggleAttackStyleResponse extends Response {
	private int attackStyleId;
	public ToggleAttackStyleResponse() {
		setAction("toggle_attack_style");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		attackStyleId = player.getDto().getAttackStyleId() + 1;
		if (attackStyleId > 3)
			attackStyleId = 1;
		
		DatabaseUpdater.enqueue(UpdatePlayerEntity.builder().id(player.getId()).attackStyleId(attackStyleId).build());
		player.getDto().setAttackStyleId(attackStyleId);
		responseMaps.addClientOnlyResponse(player, this);
	}

}
