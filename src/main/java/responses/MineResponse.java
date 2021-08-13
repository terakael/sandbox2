package responses;

import java.util.List;

import database.dao.ArtisanToolEquivalentDao;
import database.dao.ItemDao;
import database.dao.MineableDao;
import database.dao.PlayerStorageDao;
import database.dao.StatsDao;
import database.dto.MineableDto;
import processing.PathFinder;
import processing.attackable.Player;
import processing.attackable.Player.PlayerState;
import processing.managers.DepletionManager;
import processing.managers.FightManager;
import requests.MineRequest;
import requests.Request;
import types.Items;
import types.Stats;
import types.StorageTypes;

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
		
		// does the tile have something mineable on it?
		MineableDto mineable = MineableDao.getMineableDtoByTileId(player.getFloor(), request.getTileId());
		if (mineable == null) {
			// this can technically happen when the player clicks a ladder, then right-clicks a rock
			// then finally selects "mine" after they have switched rooms.  Do not do anything in this case.
			return;
		}
		
		if (!PathFinder.isNextTo(player.getFloor(), player.getTileId(), request.getTileId())) {
			player.setPath(PathFinder.findPath(player.getFloor(), player.getTileId(), request.getTileId(), false));
			player.setState(PlayerState.walking);
			player.setSavedRequest(req);
			return;
		} else {			
			player.faceDirection(request.getTileId(), responseMaps);
			
			// does player have a pickaxe in their inventory?
			List<Integer> inventoryItemIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY);
			if (!inventoryItemIds.contains(Items.PICKAXE.getValue()) 
				&& !inventoryItemIds.contains(Items.GOLDEN_PICKAXE.getValue())
				&& !inventoryItemIds.contains(Items.MAGIC_PICKAXE.getValue())
				&& !inventoryItemIds.contains(Items.MAGIC_GOLDEN_PICKAXE.getValue())
				&& !inventoryItemIds.stream().anyMatch(ArtisanToolEquivalentDao.getArtisanEquivalents(Items.PICKAXE.getValue())::contains)) {
				setRecoAndResponseText(0, "you need a pickaxe in order to mine the rock.");
				responseMaps.addClientOnlyResponse(player, this);
				player.setState(PlayerState.idle);
				return;
			}
			
			// does the rock currently have ore in it?
			if (DepletionManager.isDepleted(DepletionManager.DepletionType.rock, player.getFloor(), request.getTileId())) {
				setRecoAndResponseText(0, "the rock currently contains no ore.");
				responseMaps.addClientOnlyResponse(player, this);
				player.setState(PlayerState.idle);
				return;
			}
			
			// does the player have the level to mine this?
			if (StatsDao.getStatLevelByStatIdPlayerId(Stats.MINING, player.getId()) < mineable.getLevel()) {
				setRecoAndResponseText(0, String.format("you need %d mining to mine this.", mineable.getLevel()));
				responseMaps.addClientOnlyResponse(player, this);
				player.setState(PlayerState.idle);
				return;
			}
			
			// does player have inventory space
			if (PlayerStorageDao.getFreeSlotByPlayerId(player.getId()) == -1) {
				setRecoAndResponseText(0, "your inventory is too full to mine anymore.");
				responseMaps.addClientOnlyResponse(player, this);
				player.setState(PlayerState.idle);
				return;
			}
			
//			new StartMiningResponse().process(request, player, responseMaps);
			
			// only send the message the first time; it's annoying if it gets spammed.
			if (player.getState() != PlayerState.mining) {
				setRecoAndResponseText(1, "you start mining the rock...");
				responseMaps.addClientOnlyResponse(player, this);
				
				player.setState(PlayerState.mining);
				player.setSavedRequest(req);
			}
			
			// we check like this because if, for example, the player had both a magic pickaxe and golden pickaxe
			// in their inventory, they would be using charges from the magic pickaxe but getting the speed
			// of the golden pickaxe.  Therefore we check each tier using else ifs.
			int usedPickaxe;
			if (inventoryItemIds.contains(Items.MAGIC_GOLDEN_PICKAXE.getValue())) {
				usedPickaxe = Items.MAGIC_GOLDEN_PICKAXE.getValue();
				player.setTickCounter(3);
			} else if (inventoryItemIds.contains(Items.MAGIC_PICKAXE.getValue())) {
				usedPickaxe = Items.MAGIC_PICKAXE.getValue();
				player.setTickCounter(5);
			} else if (inventoryItemIds.contains(Items.GOLDEN_PICKAXE.getValue())) {
				usedPickaxe = Items.GOLDEN_PICKAXE.getValue();
				player.setTickCounter(3);
			} else {
				// if the player has both a regular pickaxe and an artisan equivalent, show the artisan one
				usedPickaxe = inventoryItemIds.stream()
						.filter(e -> ArtisanToolEquivalentDao.getArtisanEquivalents(Items.PICKAXE.getValue()).contains(e))
						.findFirst()
						.orElse(Items.PICKAXE.getValue());
				player.setTickCounter(5);
			}
			
			responseMaps.addLocalResponse(player.getFloor(), player.getTileId(), 
					new ActionBubbleResponse(player, ItemDao.getItem(usedPickaxe)));
		}
	}

}
