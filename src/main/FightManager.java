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
import main.processing.PathFinder;
import main.processing.Player;
import main.processing.WorldProcessor;
import main.requests.Request;
import main.processing.Player.PlayerState;
import main.responses.DamageResponse;
import main.responses.DeathResponse;
import main.responses.DropResponse;
import main.responses.InventoryUpdateResponse;
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
		
		for (Fight fight : finishedFights)
			fights.remove(fight);
	}
	
	public static void addFight(Attackable fighter1, Attackable fighter2) {
		System.out.println("adding fight");
		fights.add(new Fight(fighter1, fighter2));
	}
	
	public static void cancelFight(Attackable participant) {
		for (Fight fight : fights) {
			if (fight.getFighter1() == participant || fight.getFighter2() == participant) {
				// clear the target from each of the fighters
				fight.getFighter1().setTarget(null);
				fight.getFighter2().setTarget(null);
				System.out.println("cancelling fight");
				fights.remove(fight);
				return;
			}
		}
	}

//	
//	@Getter @Setter
//	private static class FightingPlayer {
//		private Player rawPlayer = null;
//		private int hp;
//		private int maxHp;
//		private Map<String, Integer> stats;
//		private EquipmentBonusDto bonuses;
//		
//		public FightingPlayer(int id) {
//			this.rawPlayer = WorldProcessor.getPlayerById(id);
//			this.stats = StatsDao.getStatsByPlayerId(id);
//			this.hp = StatsDao.getCurrentHpByPlayerId(id);
//			this.maxHp = StatsDao.getMaxHpByPlayerId(id);
//			this.bonuses = null;
//		}
//		
//		private int getId() {
//			return rawPlayer.getId();
//		}
//		
//		public int getHit() {
//			int str = (int)Math.sqrt(stats.get("strength")) + this.bonuses.getStr();
//			//int acc = (int)Math.sqrt(stats.get("accuracy")) + this.bonuses.getAcc();
//			return rand.nextInt((int)(str * 0.15) + 1);
//		}
//		
//		public int getBlock() {
//			int def = (int)Math.sqrt(stats.get("defence")) + this.bonuses.getDef();
//			//int agil = (int)Math.sqrt(stats.get("agility")) + this.bonuses.getAgil();
//			return rand.nextInt((int)(def * 0.15) + 1);
//		}
//	}
//	
//	private static class Fight {
//		@Getter private FightingPlayer player1;
//		@Getter private FightingPlayer player2;
//		
//		private boolean player1turn = true;
//		
//		public Fight(int id1, int id2) {
//			player1 = new FightingPlayer(id1);
//			player2 = new FightingPlayer(id2);
//			player1turn = rand.nextBoolean();
//		}
//		
//		
//		//returns true if the fight is over
//		public boolean process(ResponseMaps responseMaps) {
//			Player p1 = player1.getRawPlayer();
//			Player p2 = player2.getRawPlayer();
//			
//			if (!PathFinder.isNextTo(p1.getTileId(), p2.getTileId())) {
//				p1.setTargetPlayerId(p2.getDto().getId());
//				p1.setState(PlayerState.chasing);
//				p2.setTargetPlayerId(p1.getDto().getId());
//				p2.setState(PlayerState.chasing);
//				return false;
//			}
//			
//			// update the bonuses as the players can change weapons/armour during the fight
//			player1.setBonuses(EquipmentDao.getEquipmentBonusesByPlayerId(player1.getId()));
//			player2.setBonuses(EquipmentDao.getEquipmentBonusesByPlayerId(player2.getId()));
//			
//			FightingPlayer attackingPlayer = player1turn ? player1 : player2;
//			FightingPlayer defendingPlayer = player1turn ? player2 : player1;
//			boolean fightOver = processAttack(attackingPlayer, defendingPlayer, responseMaps);
//			
//			if (fightOver) {
//				// send the death response to the dead player
//				FightingPlayer deadPlayer = player1turn ? player2 : player1;
//				
//				// clear all equipped items from dead player
//				EquipmentDao.clearAllEquppedItems(deadPlayer.getId());
//				
//				// drop all dead players items on ground
//				List<Integer> inventoryList = PlayerStorageDao.getInventoryListByPlayerId(deadPlayer.getId());
//				for (int itemId : inventoryList) {
//					if (itemId != 0)
//						GroundItemManager.add(itemId, deadPlayer.getRawPlayer().getTileId());
//				}
//				PlayerStorageDao.clearInventoryByPlayerId(deadPlayer.getId());
//				
//				Request req = new Request();
//				req.setId(deadPlayer.getId());
//				
//				// this pulls the equipped items and inventory list by playerId (set in the req above)
//				new InventoryUpdateResponse().process(req, deadPlayer.getRawPlayer(), responseMaps);
//				
//				// TODO don't use dropResponse, use a new ground_update response
//				DropResponse dropResponse = new DropResponse();
//				dropResponse.setGroundItems(GroundItemManager.getGroundItems());
//				responseMaps.addBroadcastResponse(dropResponse);
//				
//				// broadcast that the player died
//				DeathResponse deathResponse = new DeathResponse();
//				deathResponse.setId(deadPlayer.getId());
//				deathResponse.setCurrentHp(deadPlayer.getMaxHp());
//				deathResponse.setTileId(31375);
//				deadPlayer.getRawPlayer().setTileId(31375);
//				
//				p1.setState(PlayerState.idle);
//				p2.setState(PlayerState.idle);
//				
//				responseMaps.addBroadcastResponse(deathResponse);
//			}
//			
//			player1turn = !player1turn;
//			
//			return fightOver;
//		}
//	}
//	
//	private static boolean processAttack(FightingPlayer attackingPlayer, FightingPlayer defendingPlayer, ResponseMaps responseMaps) {
//		boolean fightOver = false;
//		
//		int dmg = Math.max(attackingPlayer.getHit() - defendingPlayer.getBlock(), 0);
//		
//		DamageResponse damageResponse = new DamageResponse();
//		damageResponse.setDamage(dmg);
//		damageResponse.setId(defendingPlayer.getId());// id=player being damaged; otherId=player doing the damage
//		damageResponse.setOtherId(attackingPlayer.getId());
//		
//		int newHp = defendingPlayer.getHp() - dmg;
//		if (newHp <= 0) {
//			newHp = StatsDao.getMaxHpByPlayerId(defendingPlayer.getId());
//			fightOver = true;
//		}
//		
//		defendingPlayer.setHp(newHp);
//		StatsDao.setRelativeBoostByPlayerIdStatId(defendingPlayer.getId(), 5, newHp - defendingPlayer.getMaxHp());
//		
//		responseMaps.addBroadcastResponse(damageResponse);
//	
//		return fightOver;
//	}
//	
//	private static List<Fight> fights = new ArrayList<>();
//	
//	public static void addFight(int player1Id, int player2Id) {
//		System.out.println("Fight between playerId " + player1Id + " and " + player2Id + " started!");
//		fights.add(new Fight(player1Id, player2Id));
//	}
//	
//	public static void cancelFight(int playerId) {
//		for (Fight fight : fights) {
//			if (fight.getPlayer1().getId() == playerId || fight.getPlayer2().getId() == playerId) {
//				fight.getPlayer1().getRawPlayer().setState(PlayerState.idle);
//				fight.getPlayer2().getRawPlayer().setState(PlayerState.idle);
//				fights.remove(fight);
//				return;
//			}
//		}
//	}
//	
//	public static void process(ResponseMaps responseMaps) {
//		List<Fight> finishedFights = new ArrayList<>();
//		for (Fight fight : fights) {
//			if (fight.process(responseMaps)) {// returns whether the fight is over or not
//				finishedFights.add(fight);
//			}
//		}
//		
//		// clean up finished fights
//		for (Fight fight : finishedFights)
//			fights.remove(fight);
//	}
}
