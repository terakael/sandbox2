package main.responses;

import java.util.ArrayList;

import main.database.InventoryItemDto;
import main.database.MineableDao;
import main.database.MineableDto;
import main.database.PlayerStorageDao;
import main.database.StatsDao;
import main.processing.FightManager;
import main.processing.PathFinder;
import main.processing.Player;
import main.processing.Player.PlayerState;
import main.processing.RockManager;
import main.requests.MineRequest;
import main.requests.Request;
import main.types.Items;
import main.types.Stats;
import main.types.StorageTypes;

public class MineResponse extends Response {
	public MineResponse() {
		setAction("mine");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof MineRequest))
			return;
		
		if (FightManager.fightWithFighterIsBattleLocked(player)) {
			setRecoAndResponseText(0, "you can't do that during combat.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		FightManager.cancelFight(player, responseMaps);
		
		MineRequest request = (MineRequest)req;
		
		if (!PathFinder.isNextTo(player.getTileId(), request.getTileId())) {
			player.setPath(PathFinder.findPath(player.getTileId(), request.getTileId(), false));
			player.setState(PlayerState.walking);
			player.setSavedRequest(req);
			return;
		} else {
			// does the tile have something mineable on it?
			MineableDto mineable = MineableDao.getMineableDtoByTileId(request.getTileId());
			if (mineable == null) {
				setRecoAndResponseText(0, "you can't mine this.");
				responseMaps.addClientOnlyResponse(player, this);
				return;
			}
			
			// does player have a pickaxe in their inventory?
			ArrayList<Integer> inventoryItemIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY.getValue());
			if (!inventoryItemIds.contains(Items.PICKAXE.getValue()) 
				&& !inventoryItemIds.contains(Items.GOLDEN_PICKAXE.getValue())
				&& !inventoryItemIds.contains(Items.MAGIC_PICKAXE.getValue())
				&& !inventoryItemIds.contains(Items.MAGIC_GOLDEN_PICKAXE.getValue())) {
				setRecoAndResponseText(0, "you need a pickaxe in order to mine the rock.");
				responseMaps.addClientOnlyResponse(player, this);
				return;
			}
			
			// does the rock currently have ore in it?
			if (RockManager.rockIsDepleted(request.getTileId())) {
				setRecoAndResponseText(0, "the rock currently contains no ore.");
				responseMaps.addClientOnlyResponse(player, this);
				return;
			}
			
			// does the player have the level to mine this?
			if (StatsDao.getStatLevelByStatIdPlayerId(Stats.MINING, player.getId()) < mineable.getLevel()) {
				setRecoAndResponseText(0, String.format("you need %d mining to mine this.", mineable.getLevel()));
				responseMaps.addClientOnlyResponse(player, this);
				return;
			}
			
			// does player have inventory space
			if (PlayerStorageDao.getFreeSlotByPlayerId(player.getId()) == -1) {
				setRecoAndResponseText(0, "your inventory is too full to mine anymore.");
				responseMaps.addClientOnlyResponse(player, this);
				return;
			}
			
			new StartMiningResponse().process(request, player, responseMaps);
			
			player.setState(PlayerState.mining);
			player.setSavedRequest(req);
			
			// we check like this because if, for example, the player had both a magic pickaxe and golden pickaxe
			// in their inventory, they would be using charges from the magic pickaxe but getting the speed
			// of the golden pickaxe.  Therefore we check each tier using else ifs.
			if (inventoryItemIds.contains(Items.MAGIC_GOLDEN_PICKAXE.getValue())) {
				player.setTickCounter(3);
			} else if (inventoryItemIds.contains(Items.MAGIC_PICKAXE.getValue())) {
				player.setTickCounter(5);
			} else if (inventoryItemIds.contains(Items.GOLDEN_PICKAXE.getValue())) {
				player.setTickCounter(3);
			} else {
				player.setTickCounter(5);
			}
		}
	}

}
