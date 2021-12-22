package processing.attackable;

import java.util.Collections;

import lombok.Setter;
import database.dao.ItemDao;
import database.dto.NPCDto;
import processing.managers.ConstructableManager;
import processing.managers.LocationManager;
import processing.managers.UndeadArmyManager;
import responses.BuryResponse;
import responses.NpcOutOfRangeResponse;
import responses.NpcUpdateResponse;
import responses.ResponseMaps;
import system.GroundItemManager;
import types.Items;

public class PlayerGrownZombie extends NPC {
	
	private static int maxLifetimeTicks = 50;
	private int remainingTicks;
	@Setter private Player planter;

	public PlayerGrownZombie(NPCDto dto) {
		super(dto);
		remainingTicks = maxLifetimeTicks;
	}
	
	@Override
	public void process(int currentTick, ResponseMaps responseMaps) {
		if (remainingTicks == maxLifetimeTicks)
			lastProcessedTick = currentTick - 1;
		
		int deltaTicks = currentTick - lastProcessedTick; // super.process updates the lastProcessedTick so we wanna use it here first
		super.process(currentTick, responseMaps);
		
		if (remainingTicks > 0) {
			remainingTicks -= deltaTicks;
			if (remainingTicks <= 0) {				
				NpcUpdateResponse updateResponse = new NpcUpdateResponse();
				updateResponse.setInstanceId(getInstanceId());
				updateResponse.setHp(0);
				responseMaps.addLocalResponse(floor, tileId, updateResponse);
				
				onDeath(planter, responseMaps);
			}
		}
	}
	
	@Override
	protected void handleRespawn(ResponseMaps responseMaps, int deltaTicks) {
		deathTimer -= deltaTicks;
		if (deathTimer <= 0) {
			deathTimer = 0;
			
			NpcOutOfRangeResponse outOfRangeResponse = new NpcOutOfRangeResponse();
			outOfRangeResponse.setInstances(Collections.singleton(getInstanceId()));
			responseMaps.addLocalResponse(getFloor(), getTileId(), outOfRangeResponse);
			
			LocationManager.removeNpc(this);
		}
	}
	
	@Override
	protected void handleLootDrop(Player killer, ResponseMaps responseMaps) {
		// player grown zombies should only drop bones.
		if (ConstructableManager.constructableIsInRadius(floor, tileId, 129, 3)) {
			// give the player the corresponding prayer exp instead of dropping it
			BuryResponse.handleBury(killer, Items.BONES.getValue(), responseMaps);
		} else {
			GroundItemManager.add(floor, killer.getId(), Items.BONES.getValue(), tileId, 1, ItemDao.getMaxCharges(Items.BONES.getValue()));
		}
	}
}
