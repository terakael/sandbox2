package database.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import types.PlayerPartType;

@Getter
@RequiredArgsConstructor
public class BaseAnimationsDto {
	private final int id;
	private final PlayerPartType playerPartType;
	private final int animationId;
}
