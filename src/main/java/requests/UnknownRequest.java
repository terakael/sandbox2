package requests;

public class UnknownRequest extends Request {
	@Override
	public String getAction() {
		assert(false);
		return "unknown";
	}
}
