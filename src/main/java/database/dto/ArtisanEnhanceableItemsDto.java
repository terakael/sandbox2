package database.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ArtisanEnhanceableItemsDto {
	private final int itemId;
	private final int enhancedItemId;
	private final int numPoints;
}
