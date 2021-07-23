package responses;

import database.dao.ItemDao;
import processing.attackable.Player;
import processing.attackable.Player.PlayerState;
import processing.managers.FightManager;
import requests.Request;
import types.Items;

public class ThrowResponse extends Response {

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (FightManager.fightWithFighterExists(player)) {
			setRecoAndResponseText(0, "you can't do that during combat.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		responseMaps.addClientOnlyResponse(player, MessageResponse.newMessageResponse("you throw the seed on the ground...", "white"));
		
		player.setState(PlayerState.growing_zombie);
		player.setTickCounter(5);
		
		responseMaps.addLocalResponse(player.getFloor(), player.getTileId(), 
				new ActionBubbleResponse(player, ItemDao.getItem(Items.ZOMBIE_SEEDS.getValue())));
	}
}
