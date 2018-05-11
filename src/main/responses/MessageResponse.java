package main.responses;

import lombok.Setter;
import main.database.PlayerDao;
import main.requests.MessageRequest;
import main.requests.Request;

public class MessageResponse extends Response {
	@Setter private String message;
	@Setter private String colour = "yellow";

	public MessageResponse(String action) {
		super(action);
	}

	@Override
	public boolean process(Request req) {
		if (!(req instanceof MessageRequest)) {
			setRecoAndResponseText(0, "funny business");
			return false;
		}
		
		MessageRequest messageReq = (MessageRequest)req;
		String name = PlayerDao.getNameFromId(messageReq.getId());
		
		setRecoAndResponseText(1, "");
		
		String msg = messageReq.getMessage();
		
		if (msg.length() >= 2 && msg.substring(0, 2).equals("::"))
			return false;// debug command; don't broadcast
		
		if (msg.length() > 100)
			msg = msg.substring(0, 100);
		message = String.format("%s: %s", name, msg);
		return true;// broadcast to everyone
	}

}
