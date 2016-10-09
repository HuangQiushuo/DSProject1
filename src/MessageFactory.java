import org.json.simple.JSONArray;
import org.json.simple.JSONObject;


/**
 * @author QiushuoHuang

 * 2016/9/21
 */
public class MessageFactory {

	private MessageFactory(){}
	
	@SuppressWarnings("unchecked")
	public static JSONObject getRoomChangeMessage(String userId, String formerId, String newRoomId){
		JSONObject message = new JSONObject();
		message.put(TokenCollector.TYPE, TokenCollector.ROOM_CHANGE);
		message.put(TokenCollector.IDENTITY, userId);
		message.put(TokenCollector.FORMER, formerId);
		message.put(TokenCollector.ROOM_ID, newRoomId);
		return message;
	}
	
	@SuppressWarnings("unchecked")
	public static JSONObject getCreateRoomMessage(String roomId, String approved){
		JSONObject message = new JSONObject();
		message.put(TokenCollector.TYPE, TokenCollector.CREATE_ROOM);
		message.put(TokenCollector.ROOM_ID, roomId);
		message.put(TokenCollector.APPROVED, approved);
		return message;
	}
	
	@SuppressWarnings("unchecked")
	public static JSONObject getReleaseRoomIdMessage(String roomId, String serverId,String approved){
		JSONObject message = new JSONObject();
		message.put(TokenCollector.TYPE, TokenCollector.RELEASE_ROOMID);
		message.put(TokenCollector.SERVER_ID, serverId);
		message.put(TokenCollector.ROOM_ID, roomId);
		message.put(TokenCollector.APPROVED, approved);
		return message;
	}
	
	@SuppressWarnings("unchecked")
	public static JSONObject getLockRoomIdMessage(String roomId, String serverId){
		JSONObject message = new JSONObject();
		message.put(TokenCollector.TYPE, TokenCollector.LOCK_ROOMID);
		message.put(TokenCollector.SERVER_ID, serverId);
		message.put(TokenCollector.ROOM_ID, roomId);
		return message;
	}
	
	@SuppressWarnings("unchecked")
	public static JSONObject getShowListMessage(JSONArray array){
		JSONObject message = new JSONObject();
		message.put(TokenCollector.TYPE, TokenCollector.ROOM_LIST);
		message.put(TokenCollector.ROOMS, array);
		return message;
	}
	
	@SuppressWarnings("unchecked")
	public static JSONObject getWhoMessage(String roomId, String owner, JSONArray array){
		JSONObject message = new JSONObject();
		message.put(TokenCollector.TYPE, TokenCollector.ROOM_CONTENTS);
		message.put(TokenCollector.ROOM_ID, roomId);
		message.put(TokenCollector.IDENTITIES, array);
		message.put(TokenCollector.OWNER, owner);
		return message;
	}
	
	@SuppressWarnings("unchecked")
	public static JSONObject getNewIdentityMessage(String approved){
		JSONObject message = new JSONObject();
		message.put(TokenCollector.TYPE, TokenCollector.NEW_IDENTITY);
		message.put(TokenCollector.APPROVED, approved);
		return message;
	}
	
	@SuppressWarnings("unchecked")
	public static JSONObject getReleaseIdentityMessage(String serverId, String userId){
		JSONObject message = new JSONObject();
		message.put(TokenCollector.TYPE, TokenCollector.RELEASE_IDENTITY);
		message.put(TokenCollector.SERVER_ID, serverId);
		message.put(TokenCollector.IDENTITY, userId);
		return message;
	}
	
	@SuppressWarnings("unchecked")
	public static JSONObject getLockIdentityMessage(String serverId, String userId){
		JSONObject message = new JSONObject();
		message.put(TokenCollector.TYPE, TokenCollector.LOCK_IDENTITY);
		message.put(TokenCollector.SERVER_ID, serverId);
		message.put(TokenCollector.IDENTITY, userId);
		return message;
	}
	
	@SuppressWarnings("unchecked")
	public static JSONObject getChatMessage(String userId, Object content){
		JSONObject message = new JSONObject();
		message.put(TokenCollector.TYPE, TokenCollector.MESSAGE);
		message.put(TokenCollector.IDENTITY, userId);
		message.put(TokenCollector.CONTENT, content);
		return message;
	}
	
	@SuppressWarnings("unchecked")
	public static JSONObject getRouteMessage(String roomId, String host, String port){
		JSONObject message = new JSONObject();
		message.put(TokenCollector.TYPE, TokenCollector.ROUTE);
		message.put(TokenCollector.ROOM_ID, roomId);
		message.put(TokenCollector.HOST, host);
		message.put(TokenCollector.PORT, port);
		return message;
	}
	
	@SuppressWarnings("unchecked")
	public static JSONObject getServerChangeMessage(String serverId, String approved){
		JSONObject message = new JSONObject();
		message.put(TokenCollector.TYPE, TokenCollector.SERVER_CHANGE);
		message.put(TokenCollector.APPROVED, approved);
		message.put(TokenCollector.SERVER_ID, serverId);
		return message;
	}
	
	@SuppressWarnings("unchecked")
	public static JSONObject getDeleteRoomMessage(String roomId, String approved){
		JSONObject message = new JSONObject();
		message.put(TokenCollector.TYPE, TokenCollector.DELETE_ROOM);
		message.put(TokenCollector.ROOM_ID, roomId);
		message.put(TokenCollector.APPROVED, approved);
		return message;
	}
	
	@SuppressWarnings("unchecked")
	public static JSONObject getDeleteRoomToServerMessage(String roomId,String serverId){
		JSONObject message = new JSONObject();
		message.put(TokenCollector.TYPE, TokenCollector.DELETE_ROOM);
		message.put(TokenCollector.ROOM_ID, roomId);
		message.put(TokenCollector.SERVER_ID, serverId);
		return message;
	}
	
	@SuppressWarnings("unchecked")
	public static JSONObject getLoginMessage(String userId, String approved){
		JSONObject message = new JSONObject();
		message.put(TokenCollector.TYPE, TokenCollector.LOGIN);
		message.put(TokenCollector.USERID, userId);
		message.put(TokenCollector.APPROVED, approved);
		return message;
	}
}
