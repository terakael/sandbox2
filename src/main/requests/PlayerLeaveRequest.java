package main.requests;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
public class PlayerLeaveRequest extends Request {
	@Getter private String name;
}
