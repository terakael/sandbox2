package requests;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class PlayerLeaveRequest extends Request {
	@Getter private String name;
}
