package main.responses;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import main.database.dao.ConstructableDao;
import main.database.dao.ItemDao;
import main.database.dao.PlayerStorageDao;
import main.database.dao.SceneryDao;
import main.database.dao.StatsDao;
import main.database.dto.ConstructableDto;
import main.processing.ClientResourceManager;
import main.processing.PathFinder;
import main.processing.Player;
import main.processing.Player.PlayerState;
import main.requests.ConstructionRequest;
import main.requests.Request;
import main.types.Stats;
import main.types.StorageTypes;

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

		List<Integer> invItemIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY);
		final int plankCount = Collections.frequency(invItemIds, constructable.getPlankId());
		final int barCount = Collections.frequency(invItemIds, constructable.getBarId());
		final int tertiaryCount = Collections.frequency(invItemIds, constructable.getTertiaryId());

		if (plankCount < constructable.getPlankAmount() || barCount < constructable.getBarAmount()
				|| tertiaryCount < constructable.getTertiaryAmount()) {
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

		final int existingSceneryId = SceneryDao.getSceneryIdByTileId(player.getFloor(), request.getTileId());
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

		player.setState(PlayerState.construction);
		player.setSavedRequest(req);
		player.setTickCounter(3);

		// move the player to the side so the constructable doesn't appear on top of the player
		PlayerUpdateResponse playerUpdate = new PlayerUpdateResponse();
		playerUpdate.setId(player.getId());
		playerUpdate.setTileId(newTileId);
		playerUpdate.setFaceDirection(PathFinder.getDirection(newTileId, player.getTileId()));
		responseMaps.addLocalResponse(player.getFloor(), newTileId, playerUpdate);
		player.setTileId(newTileId);

		responseMaps.addLocalResponse(player.getFloor(), player.getTileId(), 
				new ActionBubbleResponse(player, ItemDao.getItem(constructable.getToolId())));
	}

	private int findDestinationTileId(int floor, int tileId) {
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
