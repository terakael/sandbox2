package processing.attackable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import database.dao.ShipAccessoryDao;
import database.dto.InventoryItemDto;
import database.dto.ShipAccessoryDto;
import database.dto.ShipDto;
import database.entity.update.UpdatePlayerEntity;
import lombok.Getter;
import processing.managers.DatabaseUpdater;
import processing.managers.LocationManager;
import responses.PlayerUpdateResponse;
import responses.ResponseMaps;
import responses.ShipUpdateResponse;
import types.DamageTypes;
import types.Storage;

public class Ship extends Attackable {
	@Getter private final int captainId; // player who owns the ship
	@Getter private int remainingTicks;
	private final ShipDto dto;
	private int[] slots;
	private Set<Player> passengers = new HashSet<>();
	private int fishingPoints = 0;
	private int offensePoints = 0;
	private int defensePoints = 0;
	private int storagePoints = 0;
	Storage storage = null;
	
	public Storage getStorage() {
		if (storage == null) {
			storage = new Storage(25 + (storagePoints * 25));
		}
		
		return storage;
	}
	
	public Ship(int captainId, ShipDto dto) {
		this.captainId = captainId;
		this.dto = dto;
		slots = new int[dto.getSlotSize()];
		Arrays.fill(slots, 0);
	}
	
	public boolean hasFreeSlots() {
		return (int)Arrays.stream(slots).filter(e -> e == 0).count() > 0;
	}
	
	public boolean setFreeSlot(int accessoryId) {
		ShipAccessoryDto dto = ShipAccessoryDao.getAccessoryById(accessoryId);
		if (dto == null)
			return false;
		
		for (int i = 0; i < slots.length; ++i) {
			if (slots[i] == 0) {
				slots[i] = accessoryId;
				fishingPoints += dto.getFishing();
				offensePoints += dto.getOffense();
				defensePoints += dto.getDefense();
				storagePoints += dto.getStorage();
				return true;
			}
		}
		return false;
	}
	
	public int getHullSceneryId() {
		return dto.getHullSceneryId();
	}
	
	private int getMaxPassengers() {
		return (int)Arrays.stream(slots).filter(e -> e == 0).count() * 2;
	}
	
	public boolean isFull() {
		return passengers.size() >= getMaxPassengers();
	}
	
	public boolean boardPlayer(Player player) {
		if (isFull() && captainId != player.getId())
			return false;
		
		passengers.add(player);
		player.setTileId(tileId);
		DatabaseUpdater.enqueue(UpdatePlayerEntity.builder().id(player.getId()).boardedShipId(captainId).build());
		return true;
	}
	
	public boolean disembarkPlayer(Player player) {
		DatabaseUpdater.enqueue(UpdatePlayerEntity.builder().id(player.getId()).boardedShipId(0).build());
		return passengers.remove(player);
	}
	
	public void removeLoggedOutPlayer(Player player) {
		passengers.remove(player);
	}
	
	public boolean playerIsAboard(int playerId) {
		return passengers.stream().anyMatch(player -> player.getId() == playerId);
	}
	
	public void process(int tick, ResponseMaps responseMaps) {
		if (popPath(responseMaps)) {
			LocationManager.addShip(this);
			
			ShipUpdateResponse updateResponse = new ShipUpdateResponse();
			updateResponse.setCaptainId(captainId);
			updateResponse.setTileId(tileId);
			responseMaps.addLocalResponse(floor, tileId, updateResponse);
			
			passengers.forEach(player -> {
				player.setTileId(tileId);
			
				PlayerUpdateResponse playerUpdateResponse = new PlayerUpdateResponse();
				playerUpdateResponse.setId(player.getId());
				playerUpdateResponse.setTileId(getTileId());
				responseMaps.addClientOnlyResponse(player, playerUpdateResponse);
			});
		}
	}

	@Override
	public void onDeath(Attackable killer, ResponseMaps responseMaps) {
		// TODO shipwreck
	}

	@Override
	public void onKill(Attackable killed, ResponseMaps responseMaps) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onHit(int damage, DamageTypes type, ResponseMaps responseMaps) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAttack(int damage, DamageTypes type, ResponseMaps responseMaps) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setStatsAndBonuses() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getExp() {
		// TODO Auto-generated method stub
		return 0;
	}
}
