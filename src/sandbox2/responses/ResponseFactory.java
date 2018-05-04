package sandbox2.responses;

public class ResponseFactory {

	public static Response create(String action) {
		if (action == null)
			return new UnknownResponse("");
		
		if (action.equals("logon"))
			return new LogonResponse(action);
		return new UnknownResponse(action);
	}
}
