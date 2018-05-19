package main;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.websocket.Session;

import com.google.gson.Gson;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import main.database.EquipmentBonusDto;
import main.database.EquipmentDao;
import main.database.PlayerDao;
import main.database.StatsDao;
import main.responses.DamageResponse;
import main.responses.DeathResponse;

public class FightManager implements Runnable {
	private Thread thread;
	private static Gson gson = new Gson();
	private static Random rand = new Random();
	
	@Getter @Setter
	private static class FightingPlayer {
		private int id;
		private int hp;
		private int maxHp;
		private Session session;
		private Map<String, Integer> stats;
		private EquipmentBonusDto bonuses;
		
		public FightingPlayer(int id) {
			this.id = id;
			this.session = Endpoint.getSessionByPlayerId(id);
			this.stats = StatsDao.getStatsByPlayerId(id);
			this.hp = StatsDao.getCurrentHpByPlayerId(id);
			this.maxHp = StatsDao.getMaxHpByPlayerId(id);
			this.bonuses = null;
		}
		
		public int getHit() {
			int str = (int)Math.sqrt(stats.get("strength")) + this.bonuses.getStr();
			int acc = (int)Math.sqrt(stats.get("accuracy")) + this.bonuses.getAcc();
			return rand.nextInt((int)(str * 0.15) + 1);
		}
		
		public int getBlock() {
			int def = (int)Math.sqrt(stats.get("defence")) + this.bonuses.getDef();
			int agil = (int)Math.sqrt(stats.get("agility")) + this.bonuses.getAgil();
			return rand.nextInt((int)(def * 0.15) + 1);
		}
	}
	
	private static class Fight {
		@Getter private FightingPlayer player1;
		@Getter private FightingPlayer player2;
		
		private boolean player1turn = true;
		
		public Fight(int id1, int id2) {
			player1 = new FightingPlayer(id1);
			player2 = new FightingPlayer(id2);
			player1turn = rand.nextBoolean();
		}
		
		
		//returns true if the fight is over
		public boolean process() {
			if (player1.getSession() == null || player2.getSession() == null)
				return true;
			
			// update the bonuses as the players can change weapons/armour during the fight
			player1.setBonuses(EquipmentDao.getEquipmentBonusesByPlayerId(player1.getId()));
			player2.setBonuses(EquipmentDao.getEquipmentBonusesByPlayerId(player2.getId()));
			
			FightingPlayer attackingPlayer = player1turn ? player1 : player2;
			FightingPlayer defendingPlayer = player1turn ? player2 : player1;
			boolean fightOver = processAttack(attackingPlayer, defendingPlayer);
			
			if (fightOver) {
				// send the death response to the dead player
				
				DeathResponse deathResponse = new DeathResponse("dead");
				deathResponse.setId(player1turn ? player2.getId() : player1.getId());// if it's player1's turn then player2 died
				deathResponse.setCurrentHp(player1turn ? player2.getHit() : player1.getHp());
				deathResponse.setX(1000);
				deathResponse.setY(1000);
				PlayerDao.updateCurrentPosition(deathResponse.getId(), deathResponse.getX(), deathResponse.getY());
				Endpoint.sendTextToEveryone(gson.toJson(deathResponse), null, false);
			}
			
			player1turn = !player1turn;
			
			return fightOver;
		}
	}
	
	private static boolean processAttack(FightingPlayer attackingPlayer, FightingPlayer defendingPlayer) {
		boolean fightOver = false;
		
		int dmg = Math.max(attackingPlayer.getHit() - defendingPlayer.getBlock(), 0);
		
		DamageResponse damageResponse = new DamageResponse("damage");
		damageResponse.setDamage(dmg);
		damageResponse.setId(defendingPlayer.getId());// id=player being damaged; otherId=player doing the damage
		damageResponse.setOtherId(attackingPlayer.getId());
		
		int newHp = defendingPlayer.getHp() - dmg;
		if (newHp <= 0) {
			newHp = StatsDao.getMaxHpByPlayerId(defendingPlayer.getId());
			fightOver = true;
		}
		
		defendingPlayer.setHp(newHp);
		StatsDao.setRelativeBoostByPlayerIdStatId(defendingPlayer.getId(), 5, newHp - defendingPlayer.getMaxHp());
	
		Endpoint.sendTextToEveryone(gson.toJson(damageResponse), null, false);
		
		return fightOver;
	}
	
	private static List<Fight> fights = new ArrayList<>();
	
	public static void addFight(int player1Id, int player2Id) {
		fights.add(new Fight(player1Id, player2Id));
	}
	
	public static void cancelFight(int playerId) {
		for (Fight fight : fights) {
			if (fight.getPlayer1().getId() == playerId || fight.getPlayer2().getId() == playerId) {
				fights.remove(fight);
				return;
			}
		}
	}
	
	public static void process() {
		List<Fight> finishedFights = new ArrayList<>();
		for (Fight fight : fights) {
			if (fight.process()) {// returns whether the fight is over or not
				finishedFights.add(fight);
			}
		}
		
		// clean up finished fights
		for (Fight fight : finishedFights)
			fights.remove(fight);
	}

	public void run() {
		while (true) {
			try {
				process();
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void start() {
		if (thread == null) {
			thread = new Thread(this, "fightmanager");
			thread.start();
		}
	}
}
