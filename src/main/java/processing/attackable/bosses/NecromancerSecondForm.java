package processing.attackable.bosses;

import java.util.Collections;
import java.util.Set;

import database.dto.NPCDto;
import processing.PathFinder;
import processing.attackable.Player;
import processing.attackable.UndeadArmyNpc;
import processing.managers.ClientResourceManager;
import processing.managers.FightManager;
import processing.managers.LocationManager;
import processing.managers.FightManager.Fight;
import responses.CastSpellResponse;
import responses.NpcUpdateResponse;
import responses.ResponseMaps;
import types.DamageTypes;
import utils.RandomUtil;

public class NecromancerSecondForm extends UndeadArmyNpc {
	
	// necromancer second form sucks life from any players that are close.
	// this is done in the form of a spell, where the sprite is the standard ghost sprite.
	// we want the sprite frames in the correct direction between the player and the necromancer.
	private static final int spellSpriteFrameUp = 648;
	private static final int spellSpriteFrameDown = 649;
	private static final int spellSpriteFrameLeft = 650;
	private static final int spellSpriteFrameRight = 651;

	public NecromancerSecondForm(NPCDto dto, int floor, int tileId) {
		super(dto, floor, tileId);
	}

	@Override
	public void onHit(int damage, DamageTypes type, ResponseMaps responseMaps) {
		super.onHit(damage, type, responseMaps);
		
		// you hit as usual, but 50% of the time the necromancer steals life based on your hit, basically partially negating your hit
		if (RandomUtil.chance(50)) {
			Fight fight = FightManager.getFightWithFighter(this);
			if (fight == null)
				return;
			
			Set<Player> players = LocationManager.getLocalPlayers(0, getTileId(), 5);
			
			// every player adds 1/3 of the dealt damage to the necromancer, capping at six players
			// meaning if there are five players within range, opponent hits a 10, necromancer will heal 16 (truncated) (10 damage * 5 players / 3)
			int stolenDamage = (Math.min(players.size(), 6) * damage) / 3;
			
			// second form gets health from all players within five tiles
			players.forEach(player -> {
				final int spriteFrameId = getGhostSpriteFrameFromPlayerPosition(getTileId(), player.getTileId());
				responseMaps.addLocalResponse(0, getTileId(), new CastSpellResponse(player.getTileId(), instanceId, "npc", spriteFrameId, 0.5, 1)); // ghost spriteframe id
				
				// if a player was to log in while players are mid-fight with the necromancer, they need to load the ghost sprite frames.
				ClientResourceManager.addSpriteFramesAndSpriteMaps(player, Collections.singleton(spriteFrameId));
				
				player.onHit(stolenDamage, type, responseMaps);
				if (player.getCurrentHp() <= 0) {
					onKill(player, responseMaps);
					player.onDeath(this, responseMaps);
				}
			});

			currentHp = Math.min(currentHp + stolenDamage, getDto().getHp());
			
			NpcUpdateResponse updateResponse = new NpcUpdateResponse();
			updateResponse.setInstanceId(instanceId);
			updateResponse.setDamage(stolenDamage, DamageTypes.MAGIC);
			updateResponse.setHp(currentHp);
			responseMaps.addLocalResponse(floor, tileId, updateResponse);
		}
	}
	
	private int getGhostSpriteFrameFromPlayerPosition(int necromancerTileId, int playerTileId) {
		int diffX = (necromancerTileId % PathFinder.LENGTH - playerTileId % PathFinder.LENGTH);
		int diffY = Math.abs(necromancerTileId / PathFinder.LENGTH - playerTileId / PathFinder.LENGTH);
		
		if (diffX >= diffY) {
			// left or up
			if (diffX >= Math.abs(diffY)) {
				// left, so return right-facing sprite
				return spellSpriteFrameRight;
			} else {
				// up, so return down-facing sprite
				return spellSpriteFrameDown;
			}
		} else {
			// down or right
			if (diffY >= Math.abs(diffX)) {
				// down, so return up-facing sprite
				return spellSpriteFrameUp;
			} else {
				// right, so return left-facing sprite
				return spellSpriteFrameLeft;
			}
		}
	}
}
