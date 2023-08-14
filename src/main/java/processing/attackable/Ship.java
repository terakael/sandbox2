package processing.attackable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import database.dto.ShipDto;
import database.entity.update.UpdatePlayerEntity;
import lombok.Getter;
import processing.managers.DatabaseUpdater;
import processing.managers.LocationManager;
import responses.PlayerUpdateResponse;
import responses.ResponseMaps;
import responses.ShipUpdateResponse;
import types.DamageTypes;

public class Ship extends Attackable {
	@Getter private final int captainId; // player who owns the ship
	@Getter private int remainingTicks;
	private final ShipDto dto;
	private int[] slots;
	private Set<Player> passengers = new HashSet<>();
	
	public Ship(int captainId, ShipDto dto) {
		this.captainId = captainId;
		this.dto = dto;
		slots = new int[dto.getSlotSize()];
		Arrays.fill(slots, 0);
	}
	
	public int getHullSceneryId() {
		return dto.getHullSceneryId();
	}
	
	public boolean boardPlayer(Player player) {
		final int maxPassengers = (int)Arrays.stream(slots).filter(e -> e == 0).count() * 2;
		if (passengers.size() < maxPassengers) {
			passengers.add(player);
			player.setTileId(tileId);
			return true;
		}
		return false;
	}
	
	public boolean disembarkPlayer(Player player) {
		return passengers.remove(player);
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
