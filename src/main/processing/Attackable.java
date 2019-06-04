package main.processing;

import java.util.HashMap;
import java.util.Random;

import lombok.Getter;
import lombok.Setter;
import main.responses.ResponseMaps;

public abstract class Attackable {
	private static Random rand = new Random();
	
	@Setter private HashMap<String, Integer> stats = null;
	@Setter private HashMap<String, Integer> bonuses = null;
	@Setter @Getter protected int currentHp;
	@Setter @Getter protected int tileId;
	@Setter @Getter protected Attackable target = null;
	
	@Setter protected int maxCooldown = 5;
	@Setter @Getter protected int cooldown = maxCooldown;
	
	public abstract void onDeath(ResponseMaps responseMaps);
	public abstract void onKill(Attackable killed, ResponseMaps responseMaps);
	public abstract void onHit(int damage, ResponseMaps responseMaps);
	public abstract void setStatsAndBonuses();
	
	public boolean readyToHit() {
		if (cooldown == 0)
			return true;
		return --cooldown == 0;
	}
	
	public int hit() {
		cooldown = maxCooldown;
		int str = (int)Math.sqrt(stats.get("strength")) + bonuses.get("strength");
		//int acc = (int)Math.sqrt(stats.get("accuracy")) + this.bonuses.getAcc();
		return rand.nextInt((int)(str * 0.15) + 1);
	}
	
	public int block() {
		int def = (int)Math.sqrt(stats.get("defence")) + bonuses.get("defence");
		//int agil = (int)Math.sqrt(stats.get("agility")) + this.bonuses.getAgil();
		return rand.nextInt((int)(def * 0.15) + 1);
	}
}
