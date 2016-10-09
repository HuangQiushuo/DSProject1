import org.kohsuke.args4j.CmdLineParser;

/**
 * @author QiushuoHuang
 * 
 *         2016/8/21
 */
public class Launcher {

	public static void main(String[] args) {
		CmdLineArgs argsBean = new CmdLineArgs();
		CmdLineParser parser = new CmdLineParser(argsBean);
		
		//Specify the keystore details (this can be specified as VM arguments as well)
		//the keystore file contains an application's own certificate and private key
		System.setProperty("javax.net.ssl.keyStore","mykeystore/mykeystore");
		//Password to access the private key from the keystore file
		System.setProperty("javax.net.ssl.keyStorePassword","mypassword");
		// Enable debugging to view the handshake and communication which happens between the SSLClient and the SSLServer
		System.setProperty("javax.net.debug","all");
		
		try {
			parser.parseArgument(args);
			
			ServerManager serverManager = new ServerManager();
			// before running serverManager, executed initServer
			serverManager.initServer(argsBean.getServerid(),
					argsBean.getServerConfig());
			
			serverManager.run();

		} catch (Exception e) {
			e.printStackTrace();
			parser.printUsage(System.err);
		}
	}
}
