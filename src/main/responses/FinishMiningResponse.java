package main.responses;

import java.util.ArrayList;

import lombok.Setter;
import main.database.InventoryItemDto;
import main.database.ItemDao;
import main.database.MineableDao;
import main.database.MineableDto;
import main.database.PlayerStorageDao;
import main.processing.Player;
import main.processing.RockManager;
import main.requests.AddExpRequest;
import main.requests.MineRequest;
import main.requests.Request;
import main.requests.RequestFactory;
import main.types.Items;
import main.types.Stats;
import main.types.StorageTypes;
import main.utils.RandomUtil;

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
			PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.INVENTORY.getValue(), mineable.getItemId(), 1, ItemDao.getMaxCharges(mineable.getItemId()));
			
			// as there are multiple pickaxe types, there is a specific order that it uses.
			// that is: magic golden pickaxe, magic pickaxe, golden pickaxe, pickaxe
			
			// if a golden pickaxe is being used, decrease the charge by 1, and destroy it if it hits 0.
			ArrayList<Integer> invItemIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY.getValue());
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
			addExpReq.setId(player.getId());
			addExpReq.setStatId(Stats.MINING.getValue());
			addExpReq.setExp(mineable.getExp());
			
			new AddExpResponse().process(addExpReq, player, responseMaps);
			new InventoryUpdateResponse().process(RequestFactory.create("dummy", player.getId()), player, responseMaps);
			
			setResponseText(String.format("you mine some %s.", ItemDao.getNameFromId(mineable.getItemId())));
			
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
					RockManager.addDepletedRock(player.getFloor(), tileId, mineable.getRespawnTicks());
					
					RockDepleteResponse rockDepleteResponse = new RockDepleteResponse();
					rockDepleteResponse.setTileId(tileId);
					responseMaps.addLocalResponse(player.getFloor(), tileId, rockDepleteResponse);
				}
			}
		} else {
			setResponseText("you fail to hit the rock.");
		}
		
		responseMaps.addClientOnlyResponse(player, this);
	}
	
	private void decreasePickaxeCharge(int playerId, ArrayList<Integer> invItemIds, int pickaxeId) {
		InventoryItemDto invItem = PlayerStorageDao.getStorageItemFromPlayerIdAndSlot(playerId, StorageTypes.INVENTORY.getValue(), invItemIds.indexOf(pickaxeId));
		if (invItem.getCharges() > 1) {
			PlayerStorageDao.setItemFromPlayerIdAndSlot(
					playerId, 
					StorageTypes.INVENTORY.getValue(), 
					invItemIds.indexOf(pickaxeId), 
					pickaxeId, 1, invItem.getCharges() - 1);
		} else {
			PlayerStorageDao.setItemFromPlayerIdAndSlot(
					playerId, StorageTypes.INVENTORY.getValue(), invItemIds.indexOf(pickaxeId), 0, 1, 0);
		}
	}

}
