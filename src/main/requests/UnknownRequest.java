package main.requests;

public class UnknownRequest extends Request {
	@Override
	public String getAction() {
		assert(false);
		return "Unknown";
	}
}
