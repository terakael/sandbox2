package main;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import lombok.Getter;
import lombok.Setter;
import main.database.EquipmentBonusDto;
import main.database.EquipmentDao;
import main.database.PlayerStorageDao;
import main.database.StatsDao;
import main.processing.Attackable;
import main.processing.NPC;
import main.processing.PathFinder;
import main.processing.Player;
import main.processing.WorldProcessor;
import main.requests.Request;
import main.processing.Player.PlayerState;
import main.responses.DamageResponse;
import main.responses.DeathResponse;
import main.responses.DropResponse;
import main.responses.InventoryUpdateResponse;
import main.responses.PvmEndResponse;
import main.responses.PvpEndResponse;
import main.responses.PvpStartResponse;
import main.responses.ResponseMaps;

public class FightManager {
	private FightManager() {}
	
	private static Random rand = new Random();
	
	private static ArrayList<Fight> fights = new ArrayList<>();
	
	private static class Fight {
		@Getter private Attackable fighter1;
		@Getter private Attackable fighter2;
		
		Fight(Attackable fighter1, Attackable fighter2) {
			this.fighter1 = fighter1;
			this.fighter2 = fighter2;
			
			this.fighter1.setTarget(this.fighter2);
			this.fighter2.setTarget(this.fighter1);
			
			this.fighter1.setCooldown(0);
			this.fighter2.setCooldown(3);
		}
		
		public boolean process(ResponseMaps responseMaps) {
			// if the players aren't on the same tile then they are still closing in on eachother
			if (!PathFinder.isNextTo(fighter1.getTileId(), fighter2.getTileId())) {
				return false;
			}
			
			if (fighter1.readyToHit()) {
				boolean fighter2Dead = processHit(fighter1, fighter2, responseMaps);
				if (fighter2Dead) {
					fighter1.onKill(fighter2, responseMaps);
					fighter2.onDeath(responseMaps);
					return true;
				}
			}
			
			if (fighter2.readyToHit()) {
				boolean fighter1Dead = processHit(fighter2, fighter1, responseMaps);
				if (fighter1Dead) {
					fighter2.onKill(fighter1, responseMaps);
					fighter1.onDeath(responseMaps);
					return true;
				}
			}
			return false;
		}
		
		private boolean processHit(Attackable attackable, Attackable other, ResponseMaps responseMaps) {
			// update the stats/bonuses in case the player has switched weapons/armour etc
			attackable.setStatsAndBonuses();
			other.setStatsAndBonuses();
			
			int hit = Math.max(attackable.hit() - other.block(), 0);
			other.onHit(hit, responseMaps);
			
			return other.getCurrentHp() == 0;
		}
	}
	
	public static void process(ResponseMaps responseMaps) {
		ArrayList<Fight> finishedFights = new ArrayList<>();
		for (Fight fight : fights) {
			if (fight.process(responseMaps)) {
				finishedFights.add(fight);
			}
		}
		
		for (Fight fight : finishedFights) {
			cancelFight(fight.getFighter1(), responseMaps);
		}
	}
	
	public static void addFight(Attackable fighter1, Attackable fighter2) {
		fights.add(new Fight(fighter1, fighter2));
	}
	
	public static void cancelFight(Attackable participant, ResponseMaps responseMaps) {
		for (Fight fight : fights) {
			if (fight.getFighter1() == participant || fight.getFighter2() == participant) {
				// clear the target from each of the fighters
				fight.getFighter1().setTarget(null);
				fight.getFighter2().setTarget(null);
				
				// fighter1 is always a player, fighter2 can be a player or npc
				if (responseMaps != null) {// todo figure out endpoint onclose cancel fight
					if (fight.getFighter2() instanceof NPC) {
						PvmEndResponse resp = new PvmEndResponse();
						resp.setPlayerId(((Player)fight.getFighter1()).getId());
						resp.setMonsterId(((NPC)fight.getFighter2()).getId());
						resp.setTileId(fight.getFighter1().getTileId());
						responseMaps.addBroadcastResponse(resp);
					} else {
						PvpEndResponse resp = new PvpEndResponse();
						resp.setPlayer1Id(((Player)fight.getFighter1()).getId());
						resp.setPlayer2Id(((Player)fight.getFighter2()).getId());
						resp.setTileId(fight.getFighter1().getTileId());
						responseMaps.addBroadcastResponse(resp);
					}
				}
				fights.remove(fight);
				return;
			}
		}
	}
}
