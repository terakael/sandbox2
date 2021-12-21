package requests;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class MessageRequest extends Request {
	private String message;
}
