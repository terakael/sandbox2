package main.requests;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LogonRequest extends Request {
	private String name;
	private String password;
}
