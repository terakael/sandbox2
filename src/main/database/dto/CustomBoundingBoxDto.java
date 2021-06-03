package main.database.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class CustomBoundingBoxDto {
	private float xPct;
	private float yPct;
	private float wPct;
	private float hPct;
}
