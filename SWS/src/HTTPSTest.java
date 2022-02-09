import java.util.HashMap;

public class HTTPSTest extends Server {

	public static void main(String[] args) {
		HTTPSTest Server = new HTTPSTest();
		Server.setDynamic(false);
		Server.HTTPSStart(443, "./keystore.jks", "SWSTest");
	}

	@Override
	HashMap<String, byte[]> main(String req, byte[] body, byte[] additional) {
		// TODO Auto-generated method stub
		return null;
	}

}
