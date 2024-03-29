package processing.attackable;

import database.dto.NPCDto;
import processing.managers.UndeadArmyManager;
import responses.ResponseMaps;

public class UndeadArmyNpc extends NPC {

	public UndeadArmyNpc(NPCDto dto, int floor, int instanceId) {
		super(dto, floor, instanceId);
	}
	
	@Override
	protected void handleRespawn(ResponseMaps responseMaps, int deltaTicks) {
		// undead army NPCs don't respawn so we don't do anything here.
		// they'll come back with the wave reset at nightfall
		if (deathTimer > 0) {
			deathTimer -= deltaTicks;
			if (deathTimer <= 0) {
				deathTimer = 0;
				
				// when an undead army npc dies, the manager checks if all are dead, in order to begin the next wave.
				UndeadArmyManager.checkWaveStatus(responseMaps);
			}
		}
	}
}
