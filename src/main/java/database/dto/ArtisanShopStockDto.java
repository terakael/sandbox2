package database.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ArtisanShopStockDto {
	private final int itemId;
	private final int numPoints;
}
