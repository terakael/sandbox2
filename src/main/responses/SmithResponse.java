package main.responses;

import java.util.Collections;
import java.util.List;

import main.database.dao.ItemDao;
import main.database.dao.PlayerStorageDao;
import main.database.dao.SmithableDao;
import main.database.dao.StatsDao;
import main.database.dto.SmithableDto;
import main.processing.FightManager;
import main.processing.Player;
import main.processing.Player.PlayerState;
import main.requests.Request;
import main.requests.SmithRequest;
import main.types.Items;
import main.types.Stats;
import main.types.StorageTypes;

public class SmithResponse extends Response {

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof SmithRequest)) {
			player.setState(PlayerState.idle);
			return;
		}
		
		if (FightManager.fightWithFighterIsBattleLocked(player)) {
			setRecoAndResponseText(0, "you can't do that during combat.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		FightManager.cancelFight(player, responseMaps);
		
		SmithRequest smithRequest = (SmithRequest)req;
		
		// is item smithable
		SmithableDto dto = SmithableDao.getSmithableItemByItemId(smithRequest.getItemId());
		if (dto == null) {
			setRecoAndResponseText(0, "you can't smith that.");
			responseMaps.addClientOnlyResponse(player, this);
			player.setState(PlayerState.idle);
			return;
		}
		
		// does player have level to smith
		int smithingLevel = StatsDao.getStatLevelByStatIdPlayerId(Stats.SMITHING, player.getId());
		if (smithingLevel < dto.getLevel()) {
			setRecoAndResponseText(0, String.format("you need %d smithing to smith that.", dto.getLevel()));
			responseMaps.addClientOnlyResponse(player, this);
			player.setState(PlayerState.idle);
			return;
		}
		
		List<Integer> playerInvIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY);
		if (!playerInvIds.contains(ItemDao.getIdFromName("hammer"))) {
			setRecoAndResponseText(0, "you need a hammer to smith with.");
			responseMaps.addClientOnlyResponse(player, this);
			player.setState(PlayerState.idle);
			return;
		}
		
		final int playerBarCount = Collections.frequency(playerInvIds, dto.getBarId());
		if (playerBarCount < dto.getRequiredBars()) {
			// if we're already smithing, then say we've run out.
			// if we aren't smithing then we don't have the materials to start smithing.
			final String responseText = player.getState() == PlayerState.smithing 
					? "you no longer have enough bars to continue."
					: String.format("you need %d bars to smith that.", dto.getRequiredBars());
			
			setRecoAndResponseText(0, responseText);
			responseMaps.addClientOnlyResponse(player, this);
			
			player.setState(PlayerState.idle);
			return;
		}
		
		if (player.getState() != PlayerState.smithing) {
			setRecoAndResponseText(1, "you start hammering the bar...");
			responseMaps.addClientOnlyResponse(player, this);
			
			player.setState(PlayerState.smithing);
			player.setSavedRequest(req);
		}
		player.setTickCounter(5);
		
		responseMaps.addLocalResponse(player.getFloor(), player.getTileId(), 
				new ActionBubbleResponse(player, ItemDao.getItem(dto.getItemId())));
	}
}
