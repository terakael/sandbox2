package responses;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import database.dao.ArtisanToolEquivalentDao;
import database.dao.ConstructableDao;
import database.dao.ItemDao;
import database.dao.PlayerStorageDao;
import database.dao.SceneryDao;
import database.dao.StatsDao;
import database.dto.ConstructableDto;
import processing.PathFinder;
import processing.attackable.Player;
import processing.attackable.Player.PlayerState;
import processing.managers.LocationManager;
import requests.CatchRequest;
import requests.ConstructionRequest;
import requests.Request;
import types.ConstructionLandTypes;
import types.ItemAttributes;
import types.Items;
import types.Stats;
import types.StorageTypes;

public class ConstructionResponse extends WalkAndDoResponse {
	private transient ConstructableDto constructable = null;
	
	public ConstructionResponse() {
		setAction("construction");
	}
	
	@Override
	protected boolean setTarget(Request req, Player player, ResponseMaps responseMaps) {
		ConstructionRequest request = (ConstructionRequest) req;
		constructable = ConstructableDao.getConstructableBySceneryId(request.getSceneryId());
		if (constructable == null) {
			return false;
		}
		
		if ((constructable.getLandType() & ConstructionLandTypes.land.getValue()) != 0) {
			walkingTargetTileId = request.getTileId();
		}
		
		else if ((constructable.getLandType() & ConstructionLandTypes.water.getValue()) != 0) {
			// find the nearest water
			final int closestSailableTile = PathFinder.getClosestSailableTile(player.getFloor(), request.getTileId());
			if (closestSailableTile == -1) {
				setRecoAndResponseText(0, "there's no water nearby to build this on.");
				responseMaps.addClientOnlyResponse(player, this);
				return false;
			}
			
			System.out.println("found closest sailable tile at " + closestSailableTile);
			
			walkingTargetTileId = closestSailableTile;
		}
		
		return true;
	}
	
	@Override
	protected boolean nextToTarget(Request request, Player player, ResponseMaps responseMaps) {
		if ((constructable.getLandType() & ConstructionLandTypes.land.getValue()) != 0)
			return PathFinder.isNextTo(player.getFloor(), player.getTileId(), walkingTargetTileId);
		
		else if ((constructable.getLandType() & ConstructionLandTypes.water.getValue()) != 0)
			return PathFinder.isAdjacent(player.getTileId(), walkingTargetTileId);
		
		return false;
	}
	
	@Override
	protected void walkToTarget(Request request, Player player, ResponseMaps responseMaps) {
		player.setPath(PathFinder.findPath(player.getFloor(), player.getTileId(), walkingTargetTileId, true));
	}

	@Override
	protected void doAction(Request req, Player player, ResponseMaps responseMaps) {
		ConstructionRequest request = (ConstructionRequest) req;
		request.setTileId(walkingTargetTileId);
		
		if (request.isFlatpack() && (constructable.getFlatpackItemId() == 0 || ItemDao.getItem(constructable.getFlatpackItemId()) == null)) {
			setRecoAndResponseText(0, "that can't be made into a flatpack.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}

		List<Integer> invItemIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY);
		if (!invItemIds.contains(constructable.getToolId()) && !invItemIds.stream().anyMatch(ArtisanToolEquivalentDao.getArtisanEquivalents(constructable.getToolId())::contains)) {
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
			
			// if we're building on land then we're building in the place we're standing.
			// therefore we want to walk away from the spot we're on.
			if ((constructable.getLandType() & ConstructionLandTypes.land.getValue()) != 0) {
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
		}

		player.setState(PlayerState.construction);
		player.setSavedRequest(request);
		player.setTickCounter(3);
		
		// if the player has both a regular tool and an artisan tool, show the artisan tool
		final int usedToolId = invItemIds.stream()
				.filter(e -> ArtisanToolEquivalentDao.getArtisanEquivalents(constructable.getToolId()).contains(e))
				.findFirst()
				.orElse(constructable.getToolId());

		responseMaps.addLocalResponse(player.getFloor(), player.getTileId(), 
				new ActionBubbleResponse(player, ItemDao.getItem(usedToolId)));
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
				&& PathFinder.tileIsWalkable(floor, tileIdToCheck)
				&& (PathFinder.getImpassableByTileId(floor, tileIdToCheck) & 15) != 15)// impassable type 15 gets missed in isNextTo
				return tileIdToCheck;
		}

		return tileId;
	}
}
