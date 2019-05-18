package main.requests;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AddExpRequest extends Request {
	private int statId;
	private int exp;
}
