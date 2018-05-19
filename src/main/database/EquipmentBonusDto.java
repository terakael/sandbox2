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
}
