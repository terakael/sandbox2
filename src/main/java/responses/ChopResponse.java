package responses;

import java.util.List;

import database.dao.ArtisanToolEquivalentDao;
import database.dao.ChoppableDao;
import database.dao.ItemDao;
import database.dao.PlayerStorageDao;
import database.dao.SceneryDao;
import database.dto.ChoppableDto;
import processing.attackable.Player;
import processing.attackable.Player.PlayerState;
import processing.managers.ArtisanManager;
import processing.managers.DepletionManager;
import processing.managers.TybaltsTaskManager;
import processing.tybaltstasks.updates.ChopTaskUpdate;
import requests.AddExpRequest;
import requests.Request;
import types.Items;
import types.Stats;
import types.StorageTypes;
import utils.RandomUtil;

public class ChopResponse extends WalkAndDoResponse {
	private transient int sceneryId;
	private transient int usedHatchetId;
	private transient ChoppableDto choppable;
	private final boolean rechop;
	
	public ChopResponse() {
		rechop = false;
	}
	
	public ChopResponse(boolean rechop) {
		this.rechop = rechop;
	}
	
	@Override
	protected boolean setTarget(Request request, Player player, ResponseMaps responseMaps) {
		sceneryId = SceneryDao.getSceneryIdByTileId(player.getFloor(), request.getTileId());
		if (sceneryId == -1)
			return false;
		
		walkingTargetTileId = request.getTileId();
		return true;
	}

	@Override
	protected void doAction(Request request, Player player, ResponseMaps responseMaps) {
		choppable = ChoppableDao.getChoppableBySceneryId(sceneryId);
		if (choppable == null) {
			// this scenery isn't choppable
			setRecoAndResponseText(0, "you can't chop that.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		if (DepletionManager.isDepleted(DepletionManager.DepletionType.tree, player.getFloor(), request.getTileId())) {
			if (!rechop) {
				setRecoAndResponseText(0, "the tree has already been cut down.");
				responseMaps.addClientOnlyResponse(player, this);
			}
			player.setState(PlayerState.idle);
			return;
		}
			
		List<Integer> inventoryItemIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY);
		if (!inventoryItemIds.contains(Items.HATCHET.getValue()) 
				&& !inventoryItemIds.contains(Items.GOLDEN_HATCHET.getValue())
				&& !inventoryItemIds.stream().anyMatch(ArtisanToolEquivalentDao.getArtisanEquivalents(Items.HATCHET.getValue())::contains)) {
			setRecoAndResponseText(0, "you need a hatchet in order to chop the tree.");
			responseMaps.addClientOnlyResponse(player, this);
			player.setState(PlayerState.idle);
			return;
		}
		
		// does the player have the level to chop this?
		if (player.getStats().get(Stats.WOODCUTTING) < choppable.getLevel()) {
			setRecoAndResponseText(0, String.format("you need %d woodcutting to chop this.", choppable.getLevel()));
			responseMaps.addClientOnlyResponse(player, this);
			player.setState(PlayerState.idle);
			return;
		}
		
		// magic tree special case
		if (choppable.getSceneryId() == SceneryDao.getIdByName("magic tree") && !inventoryItemIds.contains(Items.GOLDEN_HATCHET.getValue())) {
			setRecoAndResponseText(0, "this hatchet doesn't seem powerful enough to chop this tree.");
			responseMaps.addClientOnlyResponse(player, this);
			player.setState(PlayerState.idle);
			return;
		}
		
		// if you have both a regular hatchet and an artisan equivalent, show the artisan equivalent
		usedHatchetId = inventoryItemIds.stream()
				.filter(e -> ArtisanToolEquivalentDao.getArtisanEquivalents(Items.HATCHET.getValue()).contains(e))
				.findFirst()
				.orElse(Items.HATCHET.getValue());
		
		// golden hatchet takes priority
		if (inventoryItemIds.contains(Items.GOLDEN_HATCHET.getValue()))
			usedHatchetId = Items.GOLDEN_HATCHET.getValue();
		
		// does player have inventory space
		if (PlayerStorageDao.getFreeSlotByPlayerId(player.getId()) == -1) {
			final String message = player.getState() == PlayerState.woodcutting
					? "your inventory is too full to chop anymore."
					: "you don't have any free inventory space.";
			
			setRecoAndResponseText(0, message);
			responseMaps.addClientOnlyResponse(player, this);
			player.setState(PlayerState.idle);
			return;
		}
		
		if (player.getState() == PlayerState.woodcutting)
			finishChopping(request, player, responseMaps);
		else
			startChopping(request, player, responseMaps);
	}
	
	private void startChopping(Request request, Player player, ResponseMaps responseMaps) {
		if (!rechop) {
			setRecoAndResponseText(1, "you start chopping the tree...");
			responseMaps.addClientOnlyResponse(player, this);
		}
		
		player.setState(PlayerState.woodcutting);
		player.setSavedRequest(request);
		
		player.setTickCounter(usedHatchetId == Items.GOLDEN_HATCHET.getValue() ? 3 : 5);
		
		responseMaps.addLocalResponse(player.getFloor(), player.getTileId(), 
				new ActionBubbleResponse(player, ItemDao.getItem(usedHatchetId)));
	}
	
	private void finishChopping(Request request, Player player, ResponseMaps responseMaps) {
		final int woodcuttingLevel = player.getStats().get(Stats.WOODCUTTING);
		final int requirementLevel = choppable.getLevel();
		
		// when woodcutting level and requirement level are equal, then there's a base X% chance of success depending on tree.
		// every subsequent level is another 1% chance of success.
		final int baseChance = 50 - (requirementLevel/2); 
		final int chance = Math.min((woodcuttingLevel - requirementLevel) + baseChance, 100);
		if (RandomUtil.chance(chance)) {
			// success
			if (usedHatchetId == Items.GOLDEN_HATCHET.getValue()) {
				int hatchetSlot = PlayerStorageDao.getSlotOfItemId(player.getId(), StorageTypes.INVENTORY, Items.GOLDEN_HATCHET.getValue());
				PlayerStorageDao.reduceCharge(player.getId(), hatchetSlot, 1);
			}
			
			AddExpRequest addExpReq = new AddExpRequest();
			addExpReq.setStatId(Stats.WOODCUTTING.getValue());
			addExpReq.setExp(choppable.getExp());
			
			new AddExpResponse().process(addExpReq, player, responseMaps);
			
			PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.INVENTORY, choppable.getLogId(), 1, ItemDao.getMaxCharges(choppable.getLogId()));
			InventoryUpdateResponse.sendUpdate(player, responseMaps);
			TybaltsTaskManager.check(player, new ChopTaskUpdate(choppable.getSceneryId()), responseMaps);
			ArtisanManager.check(player, choppable.getLogId(), responseMaps);
			
			// flat 20% chance of depletion
			if (RandomUtil.chance(20)) {
				DepletionManager.addDepletedScenery(DepletionManager.DepletionType.tree, player.getFloor(), request.getTileId(), choppable.getRespawnTicks(), responseMaps);
			}
		}
	}
}
