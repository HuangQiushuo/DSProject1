import org.kohsuke.args4j.Option;

/**
 * @author QiushuoHuang
 * 
 *         2016/8/21
 */
public class CmdLineArgs {

	@Option(required = true, name = "-n", usage = "serverid")
	private String serverid;

	@Option(required = true, name = "-l", usage = "servers config")
	private String serverConfig;
	

	public String getServerid() {
		return serverid;
	}

	public String getServerConfig() {
		return serverConfig;
	}

}
