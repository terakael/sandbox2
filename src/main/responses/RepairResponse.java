package main.responses;

import java.util.Collections;
import java.util.List;

import main.database.dao.ConstructableDao;
import main.database.dao.ItemDao;
import main.database.dao.PlayerStorageDao;
import main.database.dao.SceneryDao;
import main.database.dao.StatsDao;
import main.database.dto.ConstructableDto;
import main.processing.PathFinder;
import main.processing.attackable.Player;
import main.processing.attackable.Player.PlayerState;
import main.processing.managers.ConstructableManager;
import main.processing.managers.FightManager;
import main.processing.scenery.constructable.Constructable;
import main.requests.AddExpRequest;
import main.requests.RepairRequest;
import main.requests.Request;
import main.types.ItemAttributes;
import main.types.Stats;
import main.types.StorageTypes;

public class RepairResponse extends Response {
	public RepairResponse() {
		setAction("repair");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (FightManager.fightWithFighterIsBattleLocked(player)) {
			setRecoAndResponseText(0, "you can't do that during combat.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		FightManager.cancelFight(player, responseMaps);
		
		RepairRequest request = (RepairRequest)req;
		if (!PathFinder.isNextTo(player.getFloor(), player.getTileId(), request.getTileId())) {
			player.setPath(PathFinder.findPath(player.getFloor(), player.getTileId(), request.getTileId(), false));
			player.setState(PlayerState.walking);
			player.setSavedRequest(req);
			return;
		} else {
			Constructable constructableInstance = ConstructableManager.getConstructableInstanceByTileId(player.getFloor(), request.getTileId());
			if (constructableInstance == null)
				return;
			
			final int sceneryId = constructableInstance.getDto().getResultingSceneryId();
			ConstructableDto constructable = ConstructableDao.getConstructableBySceneryId(sceneryId);
			
			List<Integer> invItemIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY);
			final int plankCount = Collections.frequency(invItemIds, constructable.getPlankId());
			final int barCount = Collections.frequency(invItemIds, constructable.getBarId());
			
			final boolean tertiaryIsStackable = ItemDao.itemHasAttribute(constructable.getTertiaryId(), ItemAttributes.STACKABLE);
			final int tertiaryCount = tertiaryIsStackable
					? PlayerStorageDao.getStorageItemCountByPlayerIdItemIdStorageTypeId(player.getId(), constructable.getTertiaryId(), StorageTypes.INVENTORY)
					: Collections.frequency(invItemIds, constructable.getTertiaryId());
			
			if (plankCount < constructable.getPlankAmount() || barCount < constructable.getBarAmount() || tertiaryCount < constructable.getTertiaryAmount()) {
				setRecoAndResponseText(0, String.format("you need %s to repair that.", ShowConstructionMaterialsResponse.compileMaterialList(constructable)));
				responseMaps.addClientOnlyResponse(player, this);
				return;
			}
			
			if (!invItemIds.contains(constructable.getToolId())) {
				setRecoAndResponseText(0, String.format("you need a %s to repair that.", ItemDao.getNameFromId(constructable.getToolId())));
				responseMaps.addClientOnlyResponse(player, this);
				return;
			}
			
			final int constructionLevel = StatsDao.getStatLevelByStatIdPlayerId(Stats.CONSTRUCTION, player.getId());
			if (constructionLevel < constructable.getLevel()) {
				setRecoAndResponseText(0, String.format("you need %d construction to repair that.", constructable.getLevel()));
				responseMaps.addClientOnlyResponse(player, this);
				return;
			}
			
			float remainingPctOutOf100 = (float)constructableInstance.getRemainingTicks() / constructable.getLifetimeTicks();
			if (remainingPctOutOf100 > 0.2) {
				setRecoAndResponseText(0, "it doesn't seem to be in need of repair.");
				responseMaps.addClientOnlyResponse(player, this);
				return;
			}
			
			setRecoAndResponseText(1, String.format("you repair the %s.", SceneryDao.getNameById(sceneryId)));
			responseMaps.addClientOnlyResponse(player, this);
			constructableInstance.repair();
			
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
			InventoryUpdateResponse.sendUpdate(player, responseMaps);
			
			AddExpRequest addExpReq = new AddExpRequest(player.getId(), Stats.CONSTRUCTION, constructable.getExp());
			new AddExpResponse().process(addExpReq, player, responseMaps);
			
			responseMaps.addLocalResponse(player.getFloor(), player.getTileId(), 
					new ActionBubbleResponse(player, ItemDao.getItem(constructable.getToolId())));
		}
	}

}
