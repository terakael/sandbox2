package main.database.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SawmillableDto {
	private int logId;
	private int requiredLevel;
	private int exp;
	private int resultingPlankId;
}
