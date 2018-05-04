package main.requests;

public class UnknownRequest extends Request {
	@Override
	public String getAction() {
		return "Unknown";
	}
}
