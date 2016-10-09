import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerState {

	private static ServerState instance;
	
	// key is the identity of UserInfo
	private Map<String, UserInfo> connectedUsers = new ConcurrentHashMap<String, UserInfo>();
	
	// key is the id of remote server, this map does not contain this local server itself
	private Map<String, ServerInfo> servers = new ConcurrentHashMap<String, ServerInfo>();
	
	// key is the locked identity, value is serverId, indicate this identity is locked by which server
	private Map<String, String> lockedIdentities = new ConcurrentHashMap<String, String>();
	
	// key is the locked chatroomId, value is serverId, indicate this chatroomId is locked by which server
	private Map<String, String> lockedChatroomIds = new ConcurrentHashMap<String, String>();
	
	// key is the roomId of localChatroom
	private Map<String, ChatroomInfo> localChatrooms = new ConcurrentHashMap<String, ChatroomInfo>();
	
	// key is the roomId of remoteChatroom
	private Map<String, ChatroomInfo> remoteChatrooms = new ConcurrentHashMap<String, ChatroomInfo>();
	
	// server is this local sever
	private ServerInfo server;
	
	// *added attribute
	// key is the user id, and value is the corresponding password
	// indicating registration information of every whole identity
	private Map<String, String> registration = new ConcurrentHashMap<String, String>();

	
	private ServerState(){}

	public LocalChatroomInfo getMainHall() {
		return (LocalChatroomInfo) localChatrooms.get("MainHall-"
				+ server.getServerid());
	}

	public ServerInfo getServer() {
		return server;
	}

	public void setServer(ServerInfo server) {
		this.server = server;
	}

	public static synchronized ServerState getInstance() {
		if (instance == null) {
			instance = new ServerState();
		}
		return instance;
	}

	public void userConnected(UserInfo user) {
		connectedUsers.put(user.getId(), user);
	}

	public void addServer(ServerInfo server) {
		servers.put(server.getServerid(), server);
	}

	// return a list of connected users
	public Collection<UserInfo> getConnectedUsers() {
		return connectedUsers.values();
	}

	// get a user by userId
	public UserInfo getConnectedUser(String userId) {
		return connectedUsers.get(userId);
	}

	public boolean isLockedIdentity(String userId) {
		return lockedIdentities.containsKey(userId);
	}

	// return true if lock identity successful, otherwise return false
	public synchronized boolean lockIdentity(String userId, String serverId) {
		// whether it is already locked
		if (lockedIdentities.containsKey(userId)) {
			return false;
		}
		// whether it is used by others
		if (connectedUsers.containsKey(userId)) {
			return false;
		}
		lockedIdentities.put(userId, serverId);
		return true;
	}

	// return true if release identity successful, otherwise return false
	public synchronized boolean releaseIdentity(String userId, String serverId) {
		if (lockedIdentities.containsKey(userId)) {
			// if the serverId equals the server who locked this identity
			if (lockedIdentities.get(userId).equals(serverId)) {
				lockedIdentities.remove(userId);
				return true;
			}
		}
		return false;
	}

	public boolean isLockedChatroomId(String roomId) {
		return lockedChatroomIds.containsKey(roomId);
	}

	// return true if lock chatroomId successful, otherwise return false
	public synchronized boolean lockChatroomId(String roomId, String serverId) {
		// whether it is already locked
		if (lockedChatroomIds.containsKey(roomId)) {
			return false;
		}
		// whether it is used by others
		if (localChatrooms.containsKey(roomId)) {
			return false;
		}
		lockedChatroomIds.put(roomId, serverId);
		return true;
	}

	// return true if release chatroomId successful, otherwise return false
	public synchronized boolean releaseChatroomId(String roomId, String serverId) {
		
		if (lockedChatroomIds.containsKey(roomId)) {
			// if the serverId equals the server who locked this chatroomId
			if (lockedChatroomIds.get(roomId).equals(serverId)) {
				lockedChatroomIds.remove(roomId);
				return true;
			}
		}
		return false;
	}

	public void addRemoteChatroom(RemoteChatroomInfo chatroom) {
		remoteChatrooms.put(chatroom.getChatroomId(), chatroom);
	}

	public void addLocalChatroom(LocalChatroomInfo chatroom) {
		localChatrooms.put(chatroom.getChatroomId(), chatroom);
	}

	public void removeLocalChatroom(String roomid) {
		localChatrooms.remove(roomid);
	}

	public void removeRemoteChatroom(String roomid, String serverid) {
		RemoteChatroomInfo room = (RemoteChatroomInfo) remoteChatrooms
				.get(roomid);
		if (room.getManagingServer().getServerid().equals(serverid)) {
			remoteChatrooms.remove(roomid);
		}
	}

	public ChatroomInfo getLocalChatroom(String chatroomId) {
		return localChatrooms.get(chatroomId);
	}

	public ChatroomInfo getRemoteChatroom(String chatroomId) {
		return remoteChatrooms.get(chatroomId);
	}

	public ServerInfo getCordinateServer(String serverId) {
		return servers.get(serverId);
	}

	public  Collection<ChatroomInfo> getLocalChatrooms() {
		return localChatrooms.values();
	}

	public Collection<ChatroomInfo> getRemoteChatrooms() {
		return remoteChatrooms.values();
	}

	public Collection<ServerInfo> getServers() {
		return servers.values();
	}

	public int coordinatorSize() {
		return servers.size();
	}

	public synchronized void userDisconnect(String userId) {
		connectedUsers.remove(userId);
	}
	
	public void addNewRegistration(String userId, String password) {
		registration.put(userId,password);
	}
	
	public boolean isExistingUser(String userId) {
		boolean userExist = false;
		if(registration.containsKey(userId)) {
			userExist = true;
		} else {
			userExist = false;
		}
		return userExist;
	}
	
	public String getPassword(String userId) {
		return registration.get(userId);
	}
}
