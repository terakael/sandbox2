package types;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ShipContextOptions {
	board(1),
	storage(2),
	repair(4),
	cast_net(8);
	private final int value;
}
