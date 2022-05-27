import java.util.HashMap;

public class HTTPTest extends Server {
	/*
	 * Initalizer for Server
	 */
	public static void main(String[] args) {
		HTTPTest HTTPServer = new HTTPTest();
		HTTPServer.setDynamic(false); // static
		HTTPServer.HTTPStart(8080);
	}

	@Override
	HashMap<String, byte[]> main(String req, byte[] body, byte[] additional) {
		// TODO Auto-generated method stub
		return null;
	}

}
