package requests;

import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;

import com.google.gson.Gson;

public class RequestDecoder implements Decoder.Text<Request> {
	
	private static Gson gson = new Gson();

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void init(EndpointConfig arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Request decode(final String message) throws DecodeException {
		Request req = gson.fromJson(message, Request.class);
		if (req == null)
			return new UnknownRequest();
		return RequestFactory.create(req.getAction(), message);
	}

	@Override
	public boolean willDecode(String message) {
		return message != null;
	}

}
