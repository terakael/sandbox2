package responses;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

import javax.websocket.Session;

import database.dao.EquipmentDao;
import database.dao.PlayerDao;
import database.dao.PlayerStorageDao;
import database.dao.StatsDao;
import database.dto.EquipmentBonusDto;
import database.dto.InventoryItemDto;
import database.dto.PlayerDto;
import database.entity.update.UpdatePlayerEntity;
import processing.WorldProcessor;
import processing.attackable.Player;
import processing.managers.ClientResourceManager;
import processing.managers.DatabaseUpdater;
import processing.managers.LocationManager;
import processing.managers.TimeManager;
import requests.LogonRequest;
import requests.Request;
import types.StorageTypes;

@SuppressWarnings("unused")
public class LogonResponse extends Response {
	private PlayerDto playerDto;
	
	private Map<Integer, Integer> stats;
	private Map<Integer, Integer> boosts;
	private EquipmentBonusDto bonuses;

	public LogonResponse() {
		setAction("logon");
	}
	
	@Override
	protected boolean handleCombat(Request req, Player player, ResponseMaps responseMaps) {
		return true;
	}
	
	public void processLogon(Request req, Session client, ResponseMaps responseMaps) {
		if (!(req instanceof LogonRequest)) {
			setRecoAndResponseText(0, "funny business");
			return;
		}
		
		LogonRequest logonReq = (LogonRequest)req;
		
		playerDto = PlayerDao.getPlayerByUsernameAndPassword(logonReq.getName(), logonReq.getPassword());
		
		Player player = new Player(playerDto, client);
		if (playerDto == null) {
			setRecoAndResponseText(0, "invalid credentials");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		if (WorldProcessor.sessionExistsByPlayerId(playerDto.getId())) {
			setRecoAndResponseText(0, "already logged in");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		DatabaseUpdater.enqueue(UpdatePlayerEntity.builder()
				.id(player.getId())
				.lastLoggedIn(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()))
				.build());
		LocationManager.addPlayer(player);
		
		// TODO this should be done when we create the player, but the creation process doesn't exist yet
		PlayerStorageDao.initStorageForNewPlayer(playerDto.getId());
		
		stats = StatsDao.getAllStatExpByPlayerId(playerDto.getId())
				.entrySet()
				.stream()
				.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().intValue()));
		
		boosts = StatsDao.getRelativeBoostsByPlayerId(playerDto.getId())
				.entrySet()
				.stream()
				.collect(Collectors.toMap(e -> e.getKey().getValue(), Map.Entry::getValue));
		
		bonuses = EquipmentDao.getEquipmentBonusesByPlayerId(playerDto.getId());
		player.refreshBonuses(bonuses);
		player.recacheEquippedItems();
		
		// if there was a bad disconnection (server crash etc) and the player was mid-trade, put the items back into the player's inventory.
		Map<Integer, InventoryItemDto> itemsInTrade = PlayerStorageDao.getStorageDtoMapByPlayerId(playerDto.getId(), StorageTypes.TRADE);		
		for (InventoryItemDto itemInTrade : itemsInTrade.values().stream().filter(e -> e.getItemId() != 0).collect(Collectors.toList())) {
			PlayerStorageDao.addItemToFirstFreeSlot(playerDto.getId(), StorageTypes.INVENTORY, itemInTrade.getItemId(), itemInTrade.getCount(), itemInTrade.getCharges());
		}
		PlayerStorageDao.clearStorageByPlayerIdStorageTypeId(playerDto.getId(), StorageTypes.TRADE);
		
		WorldProcessor.playerSessions.put(client, player);
		responseMaps.addClientOnlyResponse(player, this);
		
		final int petId = PlayerStorageDao.getItemIdInSlot(player.getId(), StorageTypes.PET, 0);
		if (petId != 0)
			player.setPet(petId);
		
		ClientResourceManager.addAnimations(player, Collections.singleton(player.getId()));
		InventoryUpdateResponse.sendUpdate(player, responseMaps);
		new LoadPrayersResponse().process(null, player, responseMaps);
		new PlayerEnterResponse().process(null, player, responseMaps);
		
		responseMaps.addClientOnlyResponse(player, new UpdateGameTimeResponse(TimeManager.getInGameTime()));
		// if the player is underground or it's currently night time, then let the client know to set the night filter.
		if (player.getFloor() < 0 || !TimeManager.isDaytime()) {
			responseMaps.addClientOnlyResponse(player, new DaylightResponse(false, true));
		}
	}
	
	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		
	}
}
