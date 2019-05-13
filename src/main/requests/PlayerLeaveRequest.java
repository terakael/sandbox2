package main.requests;

import lombok.Getter;
import lombok.Setter;

public class PlayerLeaveRequest extends Request {
	@Setter @Getter private String name;
}
