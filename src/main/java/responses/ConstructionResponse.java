package responses;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import database.dao.ConstructableDao;
import database.dao.ItemDao;
import database.dao.PlayerStorageDao;
import database.dao.SceneryDao;
import database.dao.StatsDao;
import database.dto.ConstructableDto;
import processing.PathFinder;
import processing.attackable.Player;
import processing.attackable.Player.PlayerState;
import requests.ConstructionRequest;
import requests.Request;
import types.ItemAttributes;
import types.Stats;
import types.StorageTypes;

public class ConstructionResponse extends Response {
	public ConstructionResponse() {
		setAction("construction");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		ConstructionRequest request = (ConstructionRequest) req;

		ConstructableDto constructable = ConstructableDao.getConstructableBySceneryId(request.getSceneryId());
		if (constructable == null) {
			return;
		}
		
		if (request.isFlatpack() && (constructable.getFlatpackItemId() == 0 || ItemDao.getItem(constructable.getFlatpackItemId()) == null)) {
			setRecoAndResponseText(0, "that can't be made into a flatpack.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}

		List<Integer> invItemIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY);
		if (!invItemIds.contains(constructable.getToolId())) {
			setRecoAndResponseText(0, String.format("you need a %s to build that.", ItemDao.getNameFromId(constructable.getToolId(), false)));
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		final int plankCount = Collections.frequency(invItemIds, constructable.getPlankId());
		final int barCount = Collections.frequency(invItemIds, constructable.getBarId());
		final int tertiaryCount = ItemDao.itemHasAttribute(constructable.getTertiaryId(), ItemAttributes.STACKABLE)
				? PlayerStorageDao.getStorageItemCountByPlayerIdItemIdStorageTypeId(player.getId(), constructable.getTertiaryId(), StorageTypes.INVENTORY)
				: Collections.frequency(invItemIds, constructable.getTertiaryId());

		if (plankCount < constructable.getPlankAmount() || barCount < constructable.getBarAmount() || tertiaryCount < constructable.getTertiaryAmount()) {
			setRecoAndResponseText(0, "you don't have the correct materials to make that.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}

		final int constructionLevel = StatsDao.getStatLevelByStatIdPlayerId(Stats.CONSTRUCTION, player.getId());
		if (constructionLevel < constructable.getLevel()) {
			setRecoAndResponseText(0,
					String.format("you need %d construction to make that.", constructable.getLevel()));
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}

		if (!request.isFlatpack()) {
			int existingSceneryId = SceneryDao.getSceneryIdByTileId(player.getFloor(), request.getTileId());
			if (existingSceneryId != -1) {
				setRecoAndResponseText(0, "you can't build that here.");
				responseMaps.addClientOnlyResponse(player, this);
				return;
			}
			
			int newTileId = findDestinationTileId(player.getFloor(), player.getTileId());
			if (newTileId == player.getTileId()) {
				// somehow no tiles are free?  are you inside a tree or something?
				setRecoAndResponseText(0, "you can't build that here.");
				responseMaps.addClientOnlyResponse(player, this);
				return;
			}
			
			// move the player to the side so the constructable doesn't appear on top of the player
			PlayerUpdateResponse playerUpdate = new PlayerUpdateResponse();
			playerUpdate.setId(player.getId());
			playerUpdate.setTileId(newTileId);
			playerUpdate.setFaceDirection(PathFinder.getDirection(newTileId, player.getTileId()));
			responseMaps.addLocalResponse(player.getFloor(), newTileId, playerUpdate);
			player.setTileId(newTileId);
		}

		player.setState(PlayerState.construction);
		player.setSavedRequest(req);
		player.setTickCounter(3);

		responseMaps.addLocalResponse(player.getFloor(), player.getTileId(), 
				new ActionBubbleResponse(player, ItemDao.getItem(constructable.getToolId())));
	}

	public static int findDestinationTileId(int floor, int tileId) {
		// the order we want to check is left, bottom, right, top
		Set<Integer> tileIdsToCheck = new LinkedHashSet<>(Arrays.asList(
					tileId + 1,
					tileId + PathFinder.LENGTH,
					tileId - 1,
					tileId - PathFinder.LENGTH
				));
		
		for (int tileIdToCheck : tileIdsToCheck) {
			if (PathFinder.isNextTo(floor, tileId, tileIdToCheck) 
				&& PathFinder.tileIsValid(floor, tileIdToCheck)
				&& (PathFinder.getImpassableByTileId(floor, tileIdToCheck) & 15) != 15)// impassable type 15 gets missed in isNextTo
				return tileIdToCheck;
		}

		return tileId;
	}
}
