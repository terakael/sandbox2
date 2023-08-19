package types;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ShipContextOptions {
	board(1),
	storage(2);
	private final int value;
}
