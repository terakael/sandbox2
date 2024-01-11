package processing.attackable.bosses;

import java.util.Stack;

import database.dto.NPCDto;
import processing.PathFinder;
import processing.attackable.Attackable;
import processing.attackable.UndeadArmyNpc;
import processing.managers.FightManager;
import processing.managers.UndeadArmyManager;
import processing.managers.FightManager.Fight;
import responses.MessageResponse;
import responses.NpcUpdateResponse;
import responses.ResponseMaps;
import responses.TeleportExplosionResponse;
import types.DamageTypes;
import utils.RandomUtil;
import utils.Utils;

public class NecromancerFirstForm extends UndeadArmyNpc {
	
	private UndeadArmyNpc healTarget = null;
	private int teleportCooldownTicks = 0;

	public NecromancerFirstForm(NPCDto dto, int floor, int tileId) {
		super(dto, floor, tileId);
	}
	
	@Override
	public void process(int currentTick, ResponseMaps responseMaps) {
		super.process(currentTick, responseMaps);
		
		if (teleportCooldownTicks > 0)
			--teleportCooldownTicks;
		
		if (target == null) {
			if (healTarget != null && healTarget.isDead())
				healTarget = null;
			
			if (healTarget != null) {
				if (!PathFinder.isNextTo(floor, tileId, healTarget.getTileId())) {
					path = PathFinder.findPath(floor, tileId, healTarget.getTileId(), false);
					
					if (teleportCooldownTicks == 0) {
						// this is unfortunate but we need to get the destination tile so we know whether to teleport or walk
						@SuppressWarnings("unchecked")
						Stack<Integer> pathCopy = (Stack<Integer>) path.clone();
						if (pathCopy.size() > 1) {
							while (pathCopy.size() > 1)
								pathCopy.pop();
							int destinationTileId = pathCopy.pop();
							if (!Utils.areTileIdsWithinRadius(getTileId(), destinationTileId, 6))
								teleportToPosition(getTileId(), destinationTileId, responseMaps);
						}
					}
				} 

				if (Utils.areTileIdsWithinRadius(getTileId(), healTarget.getTileId(), 2)) {
//				else {
					// do a heal every three ticks
					if (currentTick % 3 == 0) {
						if (healTarget.getCurrentHp() < healTarget.getDto().getHp()) {
							int healAmount = RandomUtil.getRandom(1, 15);
							if (healTarget.getCurrentHp() + healAmount > healTarget.getDto().getHp()) {
								healAmount = healTarget.getDto().getHp() - healTarget.getCurrentHp();
							}
							healTarget.onHit(-healAmount, DamageTypes.MAGIC, responseMaps);
						} else {
							// current heal target is full health, chance to switch to a diffferent one
							healTarget = null;
						}
					}
				}
			} else {
				UndeadArmyManager.getAliveNpcsInCurrentWave().forEach(npc -> {
					if (npc.getCurrentHp() < npc.getDto().getHp() && RandomUtil.chance(10) && npc != this) {
						healTarget = npc;
						return;
					}
				});
			}
		} else {
			if (healTarget != null)
				healTarget = null; // if the necromancer gets attacked then he stops healing his target
		}
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
			updateResponse.setInstanceId(instanceId);
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
	
	@Override
	protected void setPathToRandomTileInRadius(ResponseMaps responseMaps) {
		if (healTarget != null)
			return;
		
		final int destTile = chooseRandomWalkableTile();
		if (Utils.areTileIdsWithinRadius(getTileId(), destTile, 6) || teleportCooldownTicks > 0) {
			path = PathFinder.findPath(floor, tileId, destTile, true);
		} else {
			teleportToPosition(getTileId(), destTile, responseMaps);
		}
	}
	
	private void teleportToPosition(int fromTileId, int toTileId, ResponseMaps responseMaps) {
		setTileId(toTileId);
		clearPath();
		
		responseMaps.addLocalResponse(0, fromTileId, new TeleportExplosionResponse(fromTileId));
		responseMaps.addLocalResponse(0, toTileId, new TeleportExplosionResponse(toTileId));
		
		NpcUpdateResponse updateResponse = new NpcUpdateResponse();
		updateResponse.setInstanceId(instanceId);
		updateResponse.setTileId(toTileId);
		updateResponse.setSnapToTile(true);
		responseMaps.addLocalResponse(0, toTileId, updateResponse);
		
		teleportCooldownTicks = 15 + RandomUtil.getRandom(0, 10);
	}

}
