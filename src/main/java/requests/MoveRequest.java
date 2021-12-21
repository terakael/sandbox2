package requests;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class MoveRequest extends Request {
	private int x;
	private int y;
}
