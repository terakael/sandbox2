package database.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StatsDto {
	private int playerId;
	private int statId;
	private Double exp;
	private int relativeBoost;
	
	public void addExp(double exp) {
		this.exp += exp;
	}
}
