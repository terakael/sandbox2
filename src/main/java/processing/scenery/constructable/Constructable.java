package processing.scenery.constructable;

import database.dao.ItemDao;
import database.dao.PlayerStorageDao;
import database.dao.SceneryDao;
import database.dto.ConstructableDto;
import lombok.Getter;
import processing.attackable.Player;
import processing.attackable.Player.PlayerState;
import processing.managers.HousingManager;
import processing.scenery.Scenery;
import requests.UseRequest;
import responses.ActionBubbleResponse;
import responses.MessageResponse;
import responses.ResponseMaps;
import types.Items;
import types.StorageTypes;

public class Constructable implements Scenery {
	@Getter protected ConstructableDto dto;
	@Getter protected int remainingTicks;
	protected int floor;
	protected int tileId;
	protected boolean onHousingTile;
	
	public Constructable(int floor, int tileId, int lifetimeTicks, ConstructableDto dto, boolean onHousingTile) {
		remainingTicks = lifetimeTicks;
		this.dto = dto;
		this.floor = floor;
		this.tileId = tileId;
		this.onHousingTile = onHousingTile;
	}
	
	public final void process(int tickId, ResponseMaps responseMaps) {
		if (!onHousingTile)
			--remainingTicks;
		
		if (remainingTicks > 0) {
			processConstructable(tickId, responseMaps);
		} else {
			onDestroy(responseMaps);
		}
	}
	
	public void processConstructable(int tickId, ResponseMaps responseMaps) {
		
	}
	
	public void onDestroy(ResponseMaps responseMaps) {
		
	}
	
	public void repair() {
		remainingTicks = dto.getLifetimeTicks();
	}

	@Override
	public boolean use(UseRequest request, Player player, ResponseMaps responseMaps) {
		// if we're in a house and the player is using a hammer, then destroy.
		if (HousingManager.getHouseIdFromFloorAndTileId(floor, tileId) != player.getHouseId())
			return false; // currently can't use anything on general constructables
		
		if (request.getSrc() != Items.HAMMER.getValue())
			return false;
		
		if (PlayerStorageDao.getSlotOfItemId(player.getId(), StorageTypes.INVENTORY, request.getSrc()) == -1)
			return false; // player doesn't have a hammer
		
		if (player.getState() != PlayerState.disassembling) {
			final String messageText = String.format("you swing your hammer at the %s...", SceneryDao.getNameById(dto.getResultingSceneryId()));
			responseMaps.addClientOnlyResponse(player, MessageResponse.newMessageResponse(messageText, null));
			player.setState(PlayerState.disassembling);
			player.setSavedRequest(request);
		}
		player.setTickCounter(5);
		
		responseMaps.addLocalResponse(player.getFloor(), player.getTileId(), 
				new ActionBubbleResponse(player, ItemDao.getItem(request.getSrc())));
		
		return true;
	}
}
