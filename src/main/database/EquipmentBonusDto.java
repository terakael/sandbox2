package main.database;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter @Getter @AllArgsConstructor
public class EquipmentBonusDto {
	private int acc;
	private int str;
	private int def;
	private int agil;
	private int mage;
	private int hp; // hp can generate to (player_hp + bonus_hp) i.e. over max hp
	private int speed;// weapon speed
}
