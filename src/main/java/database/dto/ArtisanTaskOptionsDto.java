package database.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ArtisanTaskOptionsDto {
	private final int id;
	private final String name;
	private final String description;
	private final int iconId;
	private final int numPoints;
}
