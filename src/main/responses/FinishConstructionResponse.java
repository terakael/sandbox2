package main.responses;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import main.database.dao.ConstructableDao;
import main.database.dao.PlayerStorageDao;
import main.database.dao.SceneryDao;
import main.database.dao.StatsDao;
import main.database.dto.ConstructableDto;
import main.processing.ClientResourceManager;
import main.processing.ConstructableManager;
import main.processing.Player;
import main.requests.AddExpRequest;
import main.requests.ConstructionRequest;
import main.requests.Request;
import main.types.Stats;
import main.types.StorageTypes;

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
		final int tertiaryCount = Collections.frequency(invItemIds, constructable.getTertiaryId());
		
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
		
		final int existingSceneryId = SceneryDao.getSceneryIdByTileId(player.getFloor(), request.getTileId());
		if (existingSceneryId != -1) {
			setRecoAndResponseText(0, "you can't build that here.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		ConstructableManager.add(player.getFloor(), request.getTileId(), constructable);
		ClientResourceManager.addLocalScenery(player, Collections.singleton(constructable.getResultingSceneryId()));
		
		Map<Integer, Set<Integer>> instances = new HashMap<>();
		instances.put(constructable.getResultingSceneryId(), Collections.singleton(request.getTileId()));
		
		ConstructableInRangeResponse inRangeResponse = new ConstructableInRangeResponse();
		inRangeResponse.setInstances(instances);
		responseMaps.addLocalResponse(player.getFloor(), request.getTileId(), inRangeResponse);

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
		for (int i = 0; i < constructable.getTertiaryAmount(); ++i) {
			int tertiaryIndex = invItemIds.indexOf(constructable.getTertiaryId());
			PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, tertiaryIndex, 0, 0, 0);
			invItemIds.set(tertiaryIndex, 0);
		}
		InventoryUpdateResponse.sendUpdate(player, responseMaps);
		
		AddExpRequest addExpReq = new AddExpRequest(player.getId(), Stats.CONSTRUCTION, constructable.getExp());
		new AddExpResponse().process(addExpReq, player, responseMaps);
	}

}
