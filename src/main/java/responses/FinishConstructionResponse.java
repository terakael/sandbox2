package responses;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import processing.managers.ArtisanManager;
import processing.managers.ClientResourceManager;
import processing.managers.ConstructableManager;
import processing.managers.TybaltsTaskManager;
import processing.tybaltstasks.updates.ConstructTaskUpdate;
import requests.AddExpRequest;
import requests.ConstructionRequest;
import requests.Request;
import types.ItemAttributes;
import types.Items;
import types.Stats;
import types.StorageTypes;

public class FinishConstructionResponse extends Response {

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		ConstructionRequest request = (ConstructionRequest)req;
		
		ConstructableDto constructable = ConstructableDao.getConstructableBySceneryId(request.getSceneryId());
		if (constructable == null) {
			return;
		}
		
		List<Integer> invItemIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY);
		final int plankCount = Collections.frequency(invItemIds, constructable.getPlankId());
		final int barCount = Collections.frequency(invItemIds, constructable.getBarId());
		
		final boolean tertiaryIsStackable = ItemDao.itemHasAttribute(constructable.getTertiaryId(), ItemAttributes.STACKABLE);
		final int tertiaryCount = tertiaryIsStackable
				? PlayerStorageDao.getStorageItemCountByPlayerIdItemIdStorageTypeId(player.getId(), constructable.getTertiaryId(), StorageTypes.INVENTORY)
				: Collections.frequency(invItemIds, constructable.getTertiaryId());
		
		if (plankCount < constructable.getPlankAmount() || barCount < constructable.getBarAmount() || tertiaryCount < constructable.getTertiaryAmount()) {
			setRecoAndResponseText(0, "you don't have the correct materials to make that.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		final int constructionLevel = StatsDao.getStatLevelByStatIdPlayerId(Stats.CONSTRUCTION, player.getId());
		if (constructionLevel < constructable.getLevel()) {
			setRecoAndResponseText(0, String.format("you need %d construction to make that.", constructable.getLevel()));
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		if (!request.isFlatpack()) {
			// not a flatpack so build the actual scenery
			final int existingSceneryId = SceneryDao.getSceneryIdByTileId(player.getFloor(), request.getTileId());
			if (existingSceneryId != -1) {
				setRecoAndResponseText(0, "you can't build that here.");
				responseMaps.addClientOnlyResponse(player, this);
				return;
			}
			
			int lifetimeTicks = constructable.getLifetimeTicks();
			int goldenHammerIndex = invItemIds.indexOf(Items.GOLDEN_HAMMER.getValue());
			if (goldenHammerIndex != -1) {
				lifetimeTicks *= 2;
				PlayerStorageDao.reduceCharge(player.getId(), Items.GOLDEN_HAMMER.getValue(), goldenHammerIndex, 1);
			}
			
			ConstructableManager.add(player.getFloor(), request.getTileId(), constructable, lifetimeTicks);
			ClientResourceManager.addLocalScenery(player, Collections.singleton(constructable.getResultingSceneryId()));
			TybaltsTaskManager.check(player, new ConstructTaskUpdate(constructable.getResultingSceneryId()), responseMaps);
			
			if (constructable.getFlatpackItemId() != 0)
				ArtisanManager.check(player, constructable.getFlatpackItemId(), responseMaps); // artisan task still counts even if its not a flatpack
			
			Map<Integer, Set<Integer>> instances = new HashMap<>();
			instances.put(constructable.getResultingSceneryId(), Collections.singleton(request.getTileId()));
			
			AddSceneryInstancesResponse inRangeResponse = new AddSceneryInstancesResponse();
			inRangeResponse.setInstances(instances);
			
			// whenever we update the scenery the doors/depleted scenery are reset, so we need to reset them.
			inRangeResponse.setOpenDoors(player.getFloor(), player.getLocalTiles());
			inRangeResponse.setDepletedScenery(player.getFloor(), player.getLocalTiles());
			responseMaps.addLocalResponse(player.getFloor(), request.getTileId(), inRangeResponse);
		} else {
			// check that we're next to the workbench
			if (SceneryDao.getSceneryIdByTileId(player.getFloor(), request.getTileId()) != 151 || !PathFinder.isNextTo(player.getFloor(), player.getTileId(), request.getTileId())) {
				setRecoAndResponseText(0, "you need to be at a workbench to make that.");
				responseMaps.addClientOnlyResponse(player, this);
				player.setState(PlayerState.idle);
				return;
			}
		}

		// get rid of all the materials
		for (int i = 0; i < constructable.getPlankAmount(); ++i) {
			int plankIndex = invItemIds.indexOf(constructable.getPlankId());
			PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, plankIndex, 0, 0, 0);
			invItemIds.set(plankIndex, 0);
		}
		for (int i = 0; i < constructable.getBarAmount(); ++i) {
			int barIndex = invItemIds.indexOf(constructable.getBarId());
			PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, barIndex, 0, 0, 0);
			invItemIds.set(barIndex, 0);
		}
		if (tertiaryIsStackable) {
			int tertiaryIndex = invItemIds.indexOf(constructable.getTertiaryId());
			PlayerStorageDao.setCountOnSlot(player.getId(), StorageTypes.INVENTORY, tertiaryIndex, tertiaryCount - constructable.getTertiaryAmount());
		} else {
			for (int i = 0; i < constructable.getTertiaryAmount(); ++i) {
				int tertiaryIndex = invItemIds.indexOf(constructable.getTertiaryId());
				PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, tertiaryIndex, 0, 0, 0);
				invItemIds.set(tertiaryIndex, 0);
			}
		}
		
		if (request.isFlatpack()) {
			// add the flatpack item to the inventory
			PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.INVENTORY, constructable.getFlatpackItemId(), 1, ItemDao.getMaxCharges(constructable.getFlatpackItemId()));
			ArtisanManager.check(player, constructable.getFlatpackItemId(), responseMaps);
		}
		
		InventoryUpdateResponse.sendUpdate(player, responseMaps);
		
		AddExpRequest addExpReq = new AddExpRequest(player.getId(), Stats.CONSTRUCTION, constructable.getExp());
		new AddExpResponse().process(addExpReq, player, responseMaps);
	}

}
