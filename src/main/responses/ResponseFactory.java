package main.responses;

public class ResponseFactory {

	public static Response create(String action) {
		if (action == null)
			return new UnknownResponse("");
		
		if (action.equals("logon"))
			return new LogonResponse(action);
		
		if (action.equals("logoff"))
			return new LogoffResponse(action);
		
		if (action.equals("move"))
			return new MoveResponse(action);
		
		if (action.equals("message"))
			return new MessageResponse(action);
		
		return new UnknownResponse(action);
	}
}
