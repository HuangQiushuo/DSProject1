import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

/**
 * @author QiushuoHuang
 * 
 *         2016/8/21
 */
public class ServerManager {
	ServerInfo server = null;
	ServerState serverState;

	// initiate server, read configFile, generate server list, and try to
	// connect other servers
	public boolean initServer(String serverid, String configPath) {
		serverState = ServerState.getInstance();
		ArrayList<String[]> configList = readConfig(configPath);
		for (String[] item : configList) {
			if (server == null) {
				// Determine where this line config is for this server itself
				if (serverid.equals(item[0])) {
					server = new ServerInfo(item[0], item[1],
							Integer.parseInt(item[2]),
							Integer.parseInt(item[3]));
					serverState.setServer(server);
					LocalChatroomInfo mainHall = new LocalChatroomInfo(
							"MainHall-" + serverid, "");
					serverState.addLocalChatroom(mainHall);
					continue;
				}
			}

			// Generate ServerInfo of other servers, add them into list
			ServerInfo serverInfo = new ServerInfo(item[0], item[1],
					Integer.parseInt(item[2]), Integer.parseInt(item[3]));
			RemoteChatroomInfo chatroom = new RemoteChatroomInfo("MainHall-"
					+ serverInfo.getServerid(), serverInfo);
			serverState.addServer(serverInfo);
			serverState.addRemoteChatroom(chatroom);

		}
		try {
			// Try to connect other servers
			connectOtherServer();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}

	// Read config information from file, return a arrayList
	private ArrayList<String[]> readConfig(String configPath) {
		ArrayList<String[]> resultList = new ArrayList<String[]>();
		try {
			InputStreamReader reader = new InputStreamReader(
					new FileInputStream(configPath));
			BufferedReader bufferedReader = new BufferedReader(reader);
			String line = null;
			while ((line = bufferedReader.readLine()) != null) {
				String[] tokens = line.split("	");
				resultList.add(tokens);
			}
			reader.close();
			return resultList;
		} catch (FileNotFoundException fnfException) {
			fnfException.printStackTrace();
		} catch (Exception exception) {
			exception.printStackTrace();
		}
		return null;
	}

	// Try to connect other servers, make sure all server have started
	private void connectOtherServer() throws IOException {
		ServerSocket coorperatorSocket = new ServerSocket(
				server.getCoordinationPort());
		ConnectionListener connectionListener = new ConnectionListener(
				coorperatorSocket);
		connectionListener.start();

		for (ServerInfo coordinator : serverState.getServers()) {
			Socket socket = null;
			while (socket == null) {
				try {
					// Try to connect other servers
					// Just use socket to connect other servers
					socket = new Socket(coordinator.getAddress(),
							coordinator.getCoordinationPort());
				} catch (Exception e) {
					// Do nothing
				}
			}
			// if connected success, close the socket
			if (socket != null) {
				socket.close();
			}
		}
	}
	
	// update existing registration information of the server
	public void updateRegistrationList() {
		try {
			InputStreamReader reader = new InputStreamReader(
					new FileInputStream("resource/registrationInfo"));
			BufferedReader bufferedReader = new BufferedReader(reader);
			String line = null;
			while ((line = bufferedReader.readLine()) != null) {
				String[] tokens = line.split("	");
				// update 
				serverState.addNewRegistration(tokens[0], tokens[1]);
				System.out.println("username: " + tokens[0] + " " + "password: " + tokens[1]);
			}
			reader.close();
		} catch (FileNotFoundException fnfException) {
			fnfException.printStackTrace();
		} catch (Exception exception) {
			exception.printStackTrace();
		}
	}

	// Start working for clients
	public void run() throws IOException {
		@SuppressWarnings("resource")
		//ServerSocket serverSocket = new ServerSocket(server.getClientPort());
		//Create SSL server socket
		SSLServerSocketFactory sslserversocketfactory = (SSLServerSocketFactory) SSLServerSocketFactory
				.getDefault();
		SSLServerSocket sslserversocket = (SSLServerSocket) sslserversocketfactory.createServerSocket(server.getClientPort());
		
		
		System.out.println("waiting for connection");
		updateRegistrationList();
		while (true) {
			SSLSocket clientsslsocket = (SSLSocket) sslserversocket.accept();
			System.out.println("get a client connection");
			new UserThread(clientsslsocket).start();
		}
	}
}