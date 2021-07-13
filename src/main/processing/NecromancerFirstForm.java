package main.processing;

import main.database.dto.NPCDto;
import main.processing.FightManager.Fight;
import main.responses.MessageResponse;
import main.responses.NpcUpdateResponse;
import main.responses.ResponseMaps;
import main.responses.TeleportExplosionResponse;
import main.types.DamageTypes;

public class NecromancerFirstForm extends UndeadArmyNpc {

	public NecromancerFirstForm(NPCDto dto) {
		super(dto);
	}
	
	@Override
	public void onHit(int damage, DamageTypes type, ResponseMaps responseMaps) {
		if (UndeadArmyManager.getNumAliveNpcsInCurrentWave() > 1) {
			// if there's any other npcs alive in the wave, then reflect all damage.
			Fight fight = FightManager.getFightWithFighter(this);
			if (fight == null)
				return;
			
			Attackable opponent = fight.getFighter1() == this ? fight.getFighter2() : fight.getFighter1();
			opponent.onHit(damage, type, responseMaps);
			
			if (opponent.getCurrentHp() <= 0) {
				onKill(opponent, responseMaps);
				opponent.onDeath(this, responseMaps);
			}
		
		} else {
			currentHp -= damage;
			NpcUpdateResponse updateResponse = new NpcUpdateResponse();
			updateResponse.setInstanceId(getDto().getTileId());
			updateResponse.setDamage(damage, type);
			updateResponse.setHp(currentHp);
			responseMaps.addLocalResponse(floor, tileId, updateResponse);
			
			if (currentHp < getDto().getHp() / 4) {
				responseMaps.addLocalResponse(0, getTileId(), new TeleportExplosionResponse(getTileId()));
				FightManager.cancelFight(this, responseMaps);
				
				UndeadArmyManager.newWaveAfterTimer(10, responseMaps); // wait a few seconds before spawning the ents
				responseMaps.addLocalResponse(0, getTileId(), MessageResponse.newMessageResponse("necromancer: i'll let my ents deal with you...", "yellow"));
			}
		}
	}

}
