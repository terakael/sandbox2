package database.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SmeltableDto {
	private int barId;
	private int level;
	private int oreId;
	private int requiredCoal;
	private int requiredOre;
	private int exp;
}
