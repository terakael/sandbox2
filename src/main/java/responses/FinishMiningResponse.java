package responses;

import java.util.List;

import database.dao.ItemDao;
import database.dao.MineableDao;
import database.dao.PlayerStorageDao;
import database.dto.InventoryItemDto;
import database.dto.MineableDto;
import lombok.Setter;
import processing.attackable.Player;
import processing.managers.ArtisanManager;
import processing.managers.DepletionManager;
import processing.managers.TybaltsTaskManager;
import processing.tybaltstasks.updates.MineTaskUpdate;
import requests.AddExpRequest;
import requests.MineRequest;
import requests.Request;
import requests.RequestFactory;
import types.Items;
import types.Stats;
import types.StorageTypes;
import utils.RandomUtil;

public class FinishMiningResponse extends Response {
	@Setter private int tileId;

	public FinishMiningResponse() {
		setAction("finish_mining");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof MineRequest))
			return;
		
		MineRequest request = (MineRequest)req;
		MineableDto mineable = MineableDao.getMineableDtoByTileId(player.getFloor(), request.getTileId());
		if (mineable == null) {
			MineResponse mineResponse = new MineResponse();
			mineResponse.setRecoAndResponseText(0, "you can't mine that.");
			responseMaps.addClientOnlyResponse(player, mineResponse);
			return;
		}
		
		tileId = request.getTileId();// the tile we just finished mining
		
		// chance of failure works like this:
		// at the level of mining it, that is the pct chance of failing
		// e.g. iron is 15; at 15 mining you have 15% chance of failing.
		// rune is 75; at 75 mining you have a 75% chance of failing.
		// every level above the base level is a % less chance of failing.
		// e.g. at 80 mining, rune has a (75-5)% chance of failing, and addy has a (60-20)% chance of failing.
		int miningLevel = player.getStats().get(Stats.MINING);
		int requirementLevel = mineable.getLevel();
		
		int chance = requirementLevel;
		chance -= (miningLevel - requirementLevel);
		
		boolean mineSuccessful = chance <= 0;
		if (!mineSuccessful) {
			mineSuccessful = RandomUtil.getRandom(0, 100) > chance;
		}
		
		if (mineSuccessful) {
			final boolean minedGoldChips = RandomUtil.chance(mineable.getGoldChance());
			if (minedGoldChips) {
				// mine a gold chip
				PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.INVENTORY, Items.GOLD_CHIPS.getValue(), 1, 0);
				TybaltsTaskManager.check(player, new MineTaskUpdate(Items.GOLD_CHIPS.getValue(), 1), responseMaps);
			} else {
				// mine the rock
				PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.INVENTORY, mineable.getItemId(), 1, ItemDao.getMaxCharges(mineable.getItemId()));
				TybaltsTaskManager.check(player, new MineTaskUpdate(mineable.getItemId(), 1), responseMaps);
			}
			ArtisanManager.check(player, minedGoldChips ? Items.GOLD_CHIPS.getValue() : mineable.getItemId(), responseMaps);
			// as there are multiple pickaxe types, there is a specific order that it uses.
			// that is: magic golden pickaxe, magic pickaxe, golden pickaxe, pickaxe
			
			// if a golden pickaxe is being used, decrease the charge by 1, and destroy it if it hits 0.
			List<Integer> invItemIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY);
			if (invItemIds.contains(Items.MAGIC_GOLDEN_PICKAXE.getValue())) {
				decreasePickaxeCharge(player.getId(), invItemIds, Items.MAGIC_GOLDEN_PICKAXE.getValue());
			}
			else if (invItemIds.contains(Items.MAGIC_PICKAXE.getValue())) {
				decreasePickaxeCharge(player.getId(), invItemIds, Items.MAGIC_PICKAXE.getValue());
			}
			else if (invItemIds.contains(Items.GOLDEN_PICKAXE.getValue())) {
				decreasePickaxeCharge(player.getId(), invItemIds, Items.GOLDEN_PICKAXE.getValue());
			}
			
			AddExpRequest addExpReq = new AddExpRequest();
			addExpReq.setStatId(Stats.MINING.getValue());
			addExpReq.setExp(mineable.getExp());
			
			new AddExpResponse().process(addExpReq, player, responseMaps);
			new InventoryUpdateResponse().process(RequestFactory.create("dummy", player.getId()), player, responseMaps);
			
			setResponseText(String.format("you mine some %s.", minedGoldChips ? "gold chips" : ItemDao.getNameFromId(mineable.getItemId(), false)));
			
			// there's a chance the rock depletes on a successful mine (unless the player has a magic pickaxe variant
			if (!invItemIds.contains(Items.MAGIC_GOLDEN_PICKAXE.getValue()) && !invItemIds.contains(Items.MAGIC_PICKAXE.getValue())) {
				// rock depletion algorithm:
				// the depletion chance when you have the same mining level as the requirement rock is 20% + floor(mining req * 0.1)%
				// for example
				// copper is 20% + 0.1% = 20%
				// iron is 20% + 1.5% = 21%
				// coal is 20% + 3% = 23%
				// mithril is 20% + 4.5% = 24%
				// addy is 20% + 6% = 26%
				// rune is 20% + 7.5% = 27%
				// on top if this, half of the difference between your mining level and the req level comes off the 20%
				// e.g. mithril (45 mining req):
				// player has 45 mining:
				// 24% as stated above
				// player has 50 mining:
				// 20% - ((50 - 45) / 2)
				// = 20% - 2 = 18%
				// player has 99 mining:
				// 20% - ((99 - 45) / 2)
				// = 20 - 27 = 0 (actually 4% because the minimum depletion chance is a 10th of the requirement level)
				
				
				int depletionChance = Math.max(20 - ((miningLevel - requirementLevel) / 2), requirementLevel / 10);
				if (RandomUtil.getRandom(0,  100) < depletionChance) {
					DepletionManager.addDepletedScenery(DepletionManager.DepletionType.rock, player.getFloor(), tileId, mineable.getRespawnTicks(), responseMaps);
				}
			}
		} else {
			// don't send anything if you miss, it's annoying.
//			setResponseText("you fail to hit the rock.");
		}
		
		responseMaps.addClientOnlyResponse(player, this);
	}
	
	private void decreasePickaxeCharge(int playerId, List<Integer> invItemIds, int pickaxeId) {
		InventoryItemDto invItem = PlayerStorageDao.getStorageItemFromPlayerIdAndSlot(playerId, StorageTypes.INVENTORY, invItemIds.indexOf(pickaxeId));
		if (invItem.getCharges() > 1) {
			PlayerStorageDao.setItemFromPlayerIdAndSlot(
					playerId, 
					StorageTypes.INVENTORY, 
					invItemIds.indexOf(pickaxeId), 
					pickaxeId, 1, invItem.getCharges() - 1);
		} else {
			PlayerStorageDao.setItemFromPlayerIdAndSlot(
					playerId, StorageTypes.INVENTORY, invItemIds.indexOf(pickaxeId), ItemDao.getDegradedItemId(pickaxeId), 1, 0);
		}
	}

}
