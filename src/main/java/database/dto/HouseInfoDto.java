package database.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
public class HouseInfoDto {
	// a list of these are passed to the client when browsing houses to buy
	@Getter private final int id;
	private final String name;
	@Setter @Getter private boolean isForSale; // not final as it can be bought/sold in real-time
	private final int size; // total tile count for this house
	private final int requiredResources; // contribution points to the city nearest the house
	private final List<String> mapsBase64; // minimap segments with the house outline per floor
}
