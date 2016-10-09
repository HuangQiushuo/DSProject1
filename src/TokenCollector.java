
/**
 * @author QiushuoHuang
 * 
 *         2016/8/22
 */
/* This class contains all tokens of command
 * Using this class can avoid misspelled of commands
 * If some tokens are changed in the future, I need just change their values in this class,
 * without changing other classes.
 */
public class TokenCollector {
	// This class can't be instantiated
	private TokenCollector() {}

	public static final String IDENTITY = "identity";
	public static final String IDENTITIES = "identities";
	public static final String TYPE = "type";
	public static final String SERVER_ID = "serverid";
	public static final String NEW_IDENTITY = "newidentity";
	public static final String FORMER = "former";
	public static final String APPROVED = "approved";
	public static final String LOCKED = "locked";
	public static final String ROOM_ID = "roomid";
	public static final String ROOMS = "rooms";
	public static final String OWNER = "owner";
	public static final String CONTENT = "content";
	public static final String TRUE = "true";
	public static final String FALSE = "false";
	public static final String HOST = "host";
	public static final String PORT = "port";
	public static final String LOCK_IDENTITY = "lockidentity";
	public static final String RELEASE_IDENTITY = "releaseidentity";
	public static final String ROOM_CHANGE = "roomchange";
	public static final String LIST = "list";
	public static final String ROOM_LIST = "roomlist";
	public static final String WHO = "who";
	public static final String ROOM_CONTENTS = "roomcontents";
	public static final String CREATE_ROOM = "createroom";
	public static final String LOCK_ROOMID = "lockroomid";
	public static final String RELEASE_ROOMID = "releaseroomid";
	public static final String MOVE_JOIN = "movejoin";
	public static final String SERVER_CHANGE = "serverchange";
	public static final String ROUTE = "route";
	public static final String DELETE_ROOM = "deleteroom";
	public static final String MESSAGE = "message";
	public static final String QUIT = "quit";
	public static final String JOIN = "join";
	public static final String LOGIN = "login";
	public static final String USERID = "userid";
	public static final String PASSWORD = "password";
}