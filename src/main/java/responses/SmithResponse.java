package responses;

import java.util.Collections;
import java.util.List;

import database.dao.ArtisanToolEquivalentDao;
import database.dao.ItemDao;
import database.dao.PlayerStorageDao;
import database.dao.SmithableDao;
import database.dao.StatsDao;
import database.dto.SmithableDto;
import processing.attackable.Player;
import processing.attackable.Player.PlayerState;
import processing.managers.FightManager;
import requests.Request;
import requests.SmithRequest;
import types.Items;
import types.Stats;
import types.StorageTypes;

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
		if (!playerInvIds.contains(Items.HAMMER.getValue()) && !playerInvIds.stream().anyMatch(ArtisanToolEquivalentDao.getArtisanEquivalents(Items.HAMMER.getValue())::contains)) {
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
