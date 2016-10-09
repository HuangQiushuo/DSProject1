import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.net.ssl.SSLSocket;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * @author QiushuoHuang
 * 
 *         2016/8/21
 */
class UserThread extends Thread {
	private UserInfo userInfo;
	private BlockingQueue<JSONObject> messageQueue;
	private BufferedReader reader;
	private BufferedWriter writer;
	private ServerState serverState;
	//private Socket socket;
	private SSLSocket sslsocket;
	boolean running = true;

	public void exit() {
		running = false;
	}

	public UserThread(SSLSocket sslsocket) {
		try {
			reader = new BufferedReader(new InputStreamReader(
					sslsocket.getInputStream(), "UTF-8"));
			writer = new BufferedWriter(new OutputStreamWriter(
					sslsocket.getOutputStream(), "UTF-8"));
			serverState = ServerState.getInstance();
			messageQueue = new LinkedBlockingQueue<JSONObject>();
			this.sslsocket = sslsocket;
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void roomChange(UserInfo user, String formerId, String newRoomId)
			throws IOException {
		JSONObject message = MessageFactory.getRoomChangeMessage(user.getId(),
				formerId, newRoomId);
		LocalChatroomInfo newRoom = (LocalChatroomInfo) serverState
				.getLocalChatroom(newRoomId);
		// if newRoom == null, it means newRoom is a remote chatroom
		if (newRoom != null) {
			newRoom.addMember(user);
			user.setCurrentChatroomId(newRoomId);
			// serverState.addLocalChatroom(newRoom);
			for (UserInfo member : newRoom.getMembers()) {
				member.getManagingThread().writeMessage(message);
			}
		}

		LocalChatroomInfo formerRoom = (LocalChatroomInfo) serverState
				.getLocalChatroom(formerId);
		// if formerRoom == null, it means formerRoom is a remote chatroom
		if (formerRoom != null) {
			formerRoom.removeMember(user.getId());
			for (UserInfo member : formerRoom.getMembers()) {
				member.getManagingThread().writeMessage(message);
			}
		}
	}

	// Move all members from former chatroom to new chatroom
	private void roomChangeAll(LocalChatroomInfo formerRoom,
			LocalChatroomInfo newRoom) throws IOException {

		if (newRoom != null && formerRoom != null) {
			// Move all members from formerRoom to newRoom
			for (UserInfo member : formerRoom.getMembers()) {
				newRoom.addMember(member);
				member.setCurrentChatroomId(newRoom.getChatroomId());
			}

			// Send roomchange messages to all member in new room.
			for (UserInfo member : formerRoom.getMembers()) {
				JSONObject message = MessageFactory.getRoomChangeMessage(
						member.getId(), formerRoom.getChatroomId(),
						newRoom.getChatroomId());
				for (UserInfo user : newRoom.getMembers()) {
					user.getManagingThread().writeMessage(message);
				}
			}

			formerRoom.getMembers().clear();
		}
	}

	private void createRoom(JSONObject message) throws IOException {
		String roomId = (String) message.get("roomid");

		// Determine whether user has owned a chat room
		boolean ownedCurrentRoom = false;
		LocalChatroomInfo currentRoom = (LocalChatroomInfo) serverState
				.getLocalChatroom(userInfo.getCurrentChatroomId());
		if (currentRoom.getOwner().equals(userInfo.getId())) {
			ownedCurrentRoom = true;
		}

		// if roomId is legal or user has not owned a chat room
		if (roomId != null && roomId.matches("^[A-Za-z].+")
				&& roomId.length() >= 3 && roomId.length() <= 16
				&& !ownedCurrentRoom) {

			// Try to lock roomId
			boolean lockSuccess = lockRoomId(roomId);
			if (lockSuccess) {
				LocalChatroomInfo newRoom = new LocalChatroomInfo(roomId,
						userInfo.getId());
				serverState.addLocalChatroom(newRoom);
				JSONObject response = MessageFactory.getCreateRoomMessage(
						roomId, TokenCollector.TRUE);
				// send response message to client
				writeMessage(response);

				// change user from former room to new room
				roomChange(userInfo, userInfo.getCurrentChatroomId(), roomId);

				// release room Id
				releaseRoomId(roomId, TokenCollector.TRUE);
				return;
			} else {
				// lock unsuccessful, release roomId
				releaseRoomId(roomId, TokenCollector.FALSE);
			}
		}
		// roomId is illegal or roomId has been locked by others or user has
		// owned a chat room
		JSONObject response = MessageFactory.getCreateRoomMessage(roomId,
				TokenCollector.FALSE);
		writeMessage(response);
	}

	private void releaseRoomId(String roomId, String approved) {
		// First, release the roomId on this server itself
		serverState.releaseChatroomId(roomId, serverState.getServer()
				.getServerid());

		JSONObject message = MessageFactory.getReleaseRoomIdMessage(roomId,
				serverState.getServer().getServerid(), approved);
		// Send release message to other servers
		for (ServerInfo server : serverState.getServers()) {
			ServerCommunicatorThread communicator = new ServerCommunicatorThread(
					server.getAddress(), server.getCoordinationPort(), message);
			communicator.start();
		}
	}

	private boolean lockRoomId(String roomId) {
		// First, try to lock roomId on this server itself
		boolean lockSuccess = serverState.lockChatroomId(roomId, serverState
				.getServer().getServerid());
		if (lockSuccess == false) {
			return false;
		}

		JSONObject message = MessageFactory.getLockRoomIdMessage(roomId,
				serverState.getServer().getServerid());
		BlockingQueue<JSONObject> responseQueue = new LinkedBlockingQueue<JSONObject>();
		ArrayList<String> approveList = new ArrayList<String>();
		// Send lock roomId message to other servers
		for (ServerInfo server : serverState.getServers()) {
			ServerCommunicatorThread communicator = new ServerCommunicatorThread(
					server.getAddress(), server.getCoordinationPort(), message,
					responseQueue);
			communicator.start();
		}

		// Wait for responses from other servers
		int serverSize = serverState.getServers().size();
		for (int i = 0; i < serverSize; i++) {
			try {
				JSONObject response;
				response = responseQueue.take();
				approveList.add((String) response.get(TokenCollector.LOCKED));
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return !approveList.contains("false");
	}

	@SuppressWarnings("unchecked")
	private void showList() throws Exception {
		JSONArray array = new JSONArray();

		// Add local chat rooms into list
		for (ChatroomInfo room : serverState.getLocalChatrooms()) {
			array.add(room.getChatroomId());
		}

		// Add remote chat rooms into list
		for (ChatroomInfo room : serverState.getRemoteChatrooms()) {
			array.add(room.getChatroomId());
		}
		JSONObject message = MessageFactory.getShowListMessage(array);
		writeMessage(message);
	}

	@SuppressWarnings("unchecked")
	private void showWho() throws IOException {
		LocalChatroomInfo chatroom = (LocalChatroomInfo) serverState
				.getLocalChatroom(userInfo.getCurrentChatroomId());
		JSONArray array = new JSONArray();
		// Add chat room members into array
		for (UserInfo user : chatroom.getMembers()) {
			array.add(user.getId());
		}

		JSONObject message = MessageFactory.getWhoMessage(
				chatroom.getChatroomId(), chatroom.getOwner(), array);
		writeMessage(message);
	}

	private void addNewIdentity(JSONObject message) throws IOException {
		String userId = (String) message.get(TokenCollector.IDENTITY);

		JSONObject response;
		// Try to lock identity
		boolean lockSuccess = lockIdentity(userId);
		if (lockSuccess) {
			userInfo = new UserInfo(userId);
			userInfo.setManagingThread(this);

			// Create new identity success, add this user into connected user
			// list
			serverState.userConnected(userInfo);
			response = MessageFactory
					.getNewIdentityMessage(TokenCollector.TRUE);
			roomChange(userInfo, "", serverState.getMainHall().getChatroomId());
		} else {
			response = MessageFactory
					.getNewIdentityMessage(TokenCollector.FALSE);
		}
		writeMessage(response);

		// Release the identity
		releaseIdentity(userId);
	}

	private void releaseIdentity(String userId) {
		// First, release this server itselreleaseIdentityf.
		serverState.releaseIdentity(userId, serverState.getServer()
				.getServerid());

		// Send release message to other servers
		JSONObject message = MessageFactory.getReleaseIdentityMessage(
				serverState.getServer().getServerid(), userId);
		for (ServerInfo server : serverState.getServers()) {
			ServerCommunicatorThread communicator = new ServerCommunicatorThread(
					server.getAddress(), server.getCoordinationPort(), message);
			communicator.start();
		}
	}

	private boolean lockIdentity(String userId) {

		// First, try to lock identity on this server itself
		boolean lockSucess = serverState.lockIdentity(userId, serverState
				.getServer().getServerid());
		if (lockSucess == false) {
			return false;
		}

		JSONObject message = MessageFactory.getLockIdentityMessage(serverState
				.getServer().getServerid(), userId);
		// Send lock identity message to other servers
		BlockingQueue<JSONObject> responseQueue = new LinkedBlockingQueue<JSONObject>();
		ArrayList<String> approveList = new ArrayList<String>();
		for (ServerInfo server : serverState.getServers()) {
			ServerCommunicatorThread communicator = new ServerCommunicatorThread(
					server.getAddress(), server.getCoordinationPort(), message,
					responseQueue);
			communicator.start();
		}

		// Wait for responses from other servers
		int serverSize = serverState.getServers().size();
		for (int i = 0; i < serverSize; i++) {
			try {
				JSONObject response;
				// take n times
				response = responseQueue.take();
				approveList.add((String) response.get(TokenCollector.LOCKED));
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return !approveList.contains("false");
	}

	// Send chat messages to members in a chat room
	private void sendMessage(JSONObject message) throws IOException {
		LocalChatroomInfo chatroom = (LocalChatroomInfo) serverState
				.getLocalChatroom(userInfo.getCurrentChatroomId());
		JSONObject response = MessageFactory.getChatMessage(userInfo.getId(),
				message.get(TokenCollector.CONTENT));
		for (UserInfo user : chatroom.getMembers()) {
			if (!user.getId().equals(userInfo.getId())) {
				user.getManagingThread().writeMessage(response);
			}
		}
	}

	private void joinChatroom(JSONObject message) throws IOException {
		String userId = userInfo.getId();
		String newRoomId = (String) message.get(TokenCollector.ROOM_ID);
		String formerRoomId = userInfo.getCurrentChatroomId();
		LocalChatroomInfo formerRoom = (LocalChatroomInfo) serverState
				.getLocalChatroom(formerRoomId);

		LocalChatroomInfo localChatroom = (LocalChatroomInfo) serverState
				.getLocalChatroom(newRoomId);
		RemoteChatroomInfo remoteChatroom = (RemoteChatroomInfo) serverState
				.getRemoteChatroom(newRoomId);

		boolean noSuchRoom = false;
		if (localChatroom == null && remoteChatroom == null) {
			noSuchRoom = true;
		}

		// user is the owner of former chatroom or no such new chatroom
		if (userId.equals(formerRoom.getOwner()) || noSuchRoom) {
			JSONObject response = MessageFactory.getRoomChangeMessage(userId,
					newRoomId, newRoomId);
			writeMessage(response);
			return;
		}

		// new room is a local chatroom
		if (localChatroom != null) {
			roomChange(userInfo, formerRoomId, newRoomId);
			return;
		}

		// new room is a remote chatroom, redirect client to other server
		if (remoteChatroom != null) {
			ServerInfo serverInfo = remoteChatroom.getManagingServer();
			JSONObject response = MessageFactory.getRouteMessage(newRoomId,
					serverInfo.getAddress(),
					String.valueOf(serverInfo.getClientPort()));
			writeMessage(response);
			roomChange(userInfo, formerRoomId, newRoomId);
			serverState.userDisconnect(userId);
			exit();
		}
	}

	// user from other server join a local chat room
	private void moveJoin(JSONObject message) throws IOException {
		String userId = (String) message.get(TokenCollector.IDENTITY);
		UserInfo userInfo = new UserInfo(userId);
		userInfo.setManagingThread(this);
		this.userInfo = userInfo;
		serverState.userConnected(userInfo);

		String newRoomId = (String) message.get(TokenCollector.ROOM_ID);
		String formerRoomId = (String) message.get(TokenCollector.FORMER);
		LocalChatroomInfo newChatroom = (LocalChatroomInfo) serverState
				.getLocalChatroom(newRoomId);

		JSONObject response = MessageFactory.getServerChangeMessage(serverState
				.getServer().getServerid(), TokenCollector.TRUE);
		writeMessage(response);
		// if new chat room exist, change to new chat room
		if (newChatroom != null) {
			roomChange(userInfo, formerRoomId, newRoomId);
		}
		// otherwise, change to main hall
		else {
			roomChange(userInfo, formerRoomId, serverState.getMainHall()
					.getChatroomId());
		}
	}

	// delete room
	private void deleteRoom(JSONObject message) throws IOException {
		String roomid = (String) message.get(TokenCollector.ROOM_ID);
		LocalChatroomInfo room = (LocalChatroomInfo) serverState
				.getLocalChatroom(roomid);
		String userId = userInfo.getId();

		// if delete success
		if (room != null && room.getOwner().equals(userId)) {

			// Remove room first
			serverState.removeLocalChatroom(roomid);

			// inform other server
			JSONObject inform = MessageFactory.getDeleteRoomToServerMessage(
					roomid, serverState.getServer().getServerid());
			for (ServerInfo server : serverState.getServers()) {
				ServerCommunicatorThread communicator = new ServerCommunicatorThread(
						server.getAddress(), server.getCoordinationPort(),
						inform);
				communicator.start();
			}

			JSONObject response = MessageFactory.getDeleteRoomMessage(roomid,
					TokenCollector.TRUE);
			writeMessage(response);

			// change members of deleted room into main hall
			roomChangeAll(room, serverState.getMainHall());
		}
		// else delete failed.
		else {
			JSONObject response = MessageFactory.getDeleteRoomMessage(roomid,
					TokenCollector.FALSE);
			writeMessage(response);
		}

	}

	public void run() {
		try {
			MessageReader messageReader = new MessageReader(reader,
					messageQueue);
			messageReader.start();

			while (running) {
				JSONObject message = messageQueue.take();
				String type = (String) message.get(TokenCollector.TYPE);
				switch (type) {
				case TokenCollector.NEW_IDENTITY:
					addNewIdentity(message);
					break;
				case TokenCollector.LIST:
					showList();
					break;
				case TokenCollector.WHO:
					showWho();
					break;
				case TokenCollector.CREATE_ROOM:
					createRoom(message);
					break;
				case TokenCollector.JOIN:
					joinChatroom(message);
					break;
				case TokenCollector.MOVE_JOIN:
					moveJoin(message);
					break;
				case TokenCollector.DELETE_ROOM:
					deleteRoom(message);
					break;
				case TokenCollector.MESSAGE:
					sendMessage(message);
					break;
				case TokenCollector.QUIT:
					exit();
					quit();
					break;
				case TokenCollector.LOGIN:
					login(message);
				}
			}
			messageReader.exit();
		} catch (SocketException e) {
			exit();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	private void quit() throws IOException {
		String userId = userInfo.getId();
		serverState.userDisconnect(userId);
		LocalChatroomInfo currentRoom = (LocalChatroomInfo) serverState
				.getLocalChatroom(userInfo.getCurrentChatroomId());

		JSONObject response = MessageFactory.getRoomChangeMessage(userId, "",
				"");
		for (UserInfo memeber : currentRoom.getMembers()) {
			memeber.getManagingThread().writeMessage(response);
		}
		currentRoom.removeMember(userId);
		// if user is the owner of currentRoom, delete chat room
		if (currentRoom.getOwner().equals(userId)) {
			JSONObject message = new JSONObject();
			message.put(TokenCollector.ROOM_ID, currentRoom.getChatroomId());
			deleteRoom(message);
		}
		serverState.userDisconnect(userId);
		sslsocket.close();
	}
	
	private void login(JSONObject message) {
		String userId = (String) message.get(TokenCollector.USERID);
		String password = (String) message.get(TokenCollector.PASSWORD);
		if(!serverState.isExistingUser(userId)) {
			JSONObject response = MessageFactory.getLoginMessage(userId,
					TokenCollector.FALSE);
			writeMessage(response);
		} else if (!serverState.getPassword(userId).equals(password)) {
			JSONObject response = MessageFactory.getLoginMessage(userId,
					TokenCollector.FALSE);
			writeMessage(response);
		} else {
			JSONObject response = MessageFactory.getLoginMessage(userId,
					TokenCollector.TRUE);
			writeMessage(response);
		}
	}

	private void writeMessage(JSONObject message) {
		try {
			writer.write(message.toJSONString() + "\n");
			writer.flush();
		} catch (IOException e) {
			// Socket are closed, do nothing, run method will deal with it
		}
	}

}
