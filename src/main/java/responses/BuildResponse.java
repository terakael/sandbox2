package responses;

import java.util.List;

import database.dao.ArtisanToolEquivalentDao;
import database.dao.ItemDao;
import database.dao.PlayerStorageDao;
import database.dao.ShipAccessoryDao;
import database.dao.StatsDao;
import database.dto.ShipAccessoryDto;
import processing.PathFinder;
import processing.attackable.Player;
import processing.attackable.Ship;
import processing.attackable.Player.PlayerState;
import processing.managers.ShipManager;
import requests.BuildRequest;
import requests.Request;
import types.ItemAttributes;
import types.Items;
import types.Stats;
import types.StorageTypes;

public class BuildResponse extends WalkAndDoResponse {
	private transient ShipAccessoryDto accessory;
	private transient Ship ship;
	private transient List<Integer> invItemIds;
	
	@Override
	protected boolean nextToTarget(Request request, Player player, ResponseMaps responseMaps) {
		return PathFinder.isAdjacent(player.getTileId(), walkingTargetTileId);
	}
	
	@Override
	protected void walkToTarget(Request request, Player player, ResponseMaps responseMaps) {
		player.setPath(PathFinder.findPath(player.getFloor(), player.getTileId(), walkingTargetTileId, true));
	}

	@Override
	protected void doAction(Request req, Player player, ResponseMaps responseMaps) {
		BuildRequest request = (BuildRequest)req;
		if (request.getAccessoryId() == 0) {
			// they've clicked the boat, so show the menu
			new ShowShipAccessoriesResponse().process(null, player, responseMaps);
			return;
		}
		
		// they've selected an item from the menu, so check the materials
		else {
			if (!validate(request, player, responseMaps))
				return;
			
			player.setSavedRequest(request);
			player.setState(PlayerState.shipbuilding);
			player.setTickCounter(5);
			
			setRecoAndResponseText(1, String.format("you start building the %s...", accessory.getName()));
			responseMaps.addClientOnlyResponse(player, this);
			
			responseMaps.addLocalResponse(player.getFloor(), player.getTileId(), 
					new ActionBubbleResponse(player, ItemDao.getItem(Items.HAMMER.getValue())));
		}
	}

	@Override
	public void reprocessHook(Request req, Player player, ResponseMaps responseMaps) {
		BuildRequest request = (BuildRequest)req;
		if (!validate(request, player, responseMaps))
			return;
		
		ship.setFreeSlot(request.getAccessoryId());
		if (!ship.hasFreeSlots()) {
			// we've filled it up so finish the ship
			ShipManager.finishShip(player.getFloor(), request.getTileId(), responseMaps);
			ship.getStorage(); // inits storage if not exists
		}
		
		setRecoAndResponseText(1, String.format("you mount the %s to the ship.", accessory.getName()));
		responseMaps.addClientOnlyResponse(player, this);
		
		removeMaterial(player.getId(), accessory.getPrimaryMaterialId(), accessory.getPrimaryMaterialCount());
		removeMaterial(player.getId(), accessory.getSecondaryMaterialId(), accessory.getSecondaryMaterialCount());
		InventoryUpdateResponse.sendUpdate(player, responseMaps);
	}
	
	private void removeMaterial(int playerId, int materialId, int materialCount) {
		if (ItemDao.itemHasAttribute(materialId, ItemAttributes.STACKABLE)) {
			int primaryIndex = invItemIds.indexOf(materialId);
			PlayerStorageDao.addCountToStorageItemSlot(playerId, StorageTypes.INVENTORY, primaryIndex, -materialCount);
		} else {
			for (int i = 0; i < materialCount; ++i) {
				int primaryIndex = invItemIds.indexOf(materialId);
				PlayerStorageDao.setItemFromPlayerIdAndSlot(playerId, StorageTypes.INVENTORY, primaryIndex, 0, 0, 0);
				invItemIds.set(primaryIndex, 0);
			}
		}
	}
	
	private boolean validate(BuildRequest request, Player player, ResponseMaps responseMaps) {
		ship = ShipManager.getHullByPlayerId(player.getId());
		if (ship == null) {
			return false;
		}
		
		if (!ship.hasFreeSlots()) {
			setRecoAndResponseText(0, "there's no more room on the ship to build that.");
			responseMaps.addClientOnlyResponse(player, this);
			return false;
		}
		
		accessory = ShipAccessoryDao.getShipAccessories().stream()
				.filter(e -> e.getId() == request.getAccessoryId())
				.findFirst()
				.orElse(null);
		
		if (accessory == null)
			return false; // not an accessory id?
		
		invItemIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY);
		if (!invItemIds.contains(Items.HAMMER.getValue()) && !invItemIds.stream().anyMatch(ArtisanToolEquivalentDao.getArtisanEquivalents(Items.HAMMER.getValue())::contains)) {
			setRecoAndResponseText(0, String.format("you need a %s to build that.", ItemDao.getNameFromId(Items.HAMMER.getValue(), false)));
			responseMaps.addClientOnlyResponse(player, this);
			return false;
		}
		
		final int primaryMaterialCount = PlayerStorageDao.getStorageItemCountByPlayerIdItemIdStorageTypeId(player.getId(), accessory.getPrimaryMaterialId(), StorageTypes.INVENTORY);
		final int secondaryMaterialCount = PlayerStorageDao.getStorageItemCountByPlayerIdItemIdStorageTypeId(player.getId(), accessory.getSecondaryMaterialId(), StorageTypes.INVENTORY);

		if (primaryMaterialCount < accessory.getPrimaryMaterialCount() || secondaryMaterialCount < accessory.getSecondaryMaterialCount()) {
			setRecoAndResponseText(0, "you don't have the correct materials to build that.");
			responseMaps.addClientOnlyResponse(player, this);
			return false;
		}

		final int constructionLevel = StatsDao.getStatLevelByStatIdPlayerId(Stats.CONSTRUCTION, player.getId());
		if (constructionLevel < accessory.getLevel()) {
			setRecoAndResponseText(0,
					String.format("you need %d construction to make that.", accessory.getLevel()));
			responseMaps.addClientOnlyResponse(player, this);
			return false;
		}
		
		return true;
	}
}
