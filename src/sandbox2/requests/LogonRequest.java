package sandbox2.requests;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LogonRequest extends Request {
	private String username;
	private String password;
}
