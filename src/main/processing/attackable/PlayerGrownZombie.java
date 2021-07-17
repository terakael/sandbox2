package main.processing.attackable;

import java.util.Collections;

import lombok.Setter;
import main.GroundItemManager;
import main.database.dao.ItemDao;
import main.database.dto.NPCDto;
import main.processing.managers.ConstructableManager;
import main.processing.managers.UndeadArmyManager;
import main.responses.BuryResponse;
import main.responses.NpcOutOfRangeResponse;
import main.responses.NpcUpdateResponse;
import main.responses.ResponseMaps;
import main.types.Items;

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
			
			UndeadArmyManager.removePlayerGrownZombie(this);
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
