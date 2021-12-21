package requests;

import lombok.Getter;
import lombok.Setter;

public class TalkToRequest extends Request {
	@Setter @Getter private int objectId;
}
