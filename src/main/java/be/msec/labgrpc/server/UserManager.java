package be.msec.labgrpc.server;

import be.msec.labgrpc.User;
import be.msec.labgrpc.UserNotFoundException;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserManager {

    private static final Logger logger = Logger.getLogger(UserManager.class.getName());
    private final List<Message> messages;
    private final Map<String, User> users;

    public UserManager() {
        messages = new ArrayList<>();
        users = new HashMap<>();
    }

    public void connectUser(String username) throws DuplicateUsernameException {
        if (users.containsKey(username)) {
            throw new DuplicateUsernameException(username);
        } else {
            User user = new User(username);
            users.put(username, user);
        }
    }

    public void disconnectUser(String username) throws UserNotFoundException {
        if (users.containsKey(username)) {
            users.remove(username);
        } else {
            throw new UserNotFoundException("Could not find user: " + username);
        }
    }

    public User findUserByName(String username) throws UserNotFoundException {
        User u = users.get(username);
        if (u != null) {
            return u;
        } else throw new UserNotFoundException(username);
    }

    // add message to list and notify the synchronization method -- produce
    public void addToMessages(Message message, Object mutex) {
        synchronized (mutex) {
            try {
                info("Add message to list ...");
                messages.add(message);
                mutex.notifyAll();
                info("Release lock on message list...");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public Message getLastMessage(String userName) {
        Message msg;
        if (!messages.isEmpty()) {
            msg = messages.get(messages.size() - 1);
            // check if message is intended for user
            if (msg.getType() == MessageType.BROADCAST || (msg.getType() == MessageType.PRIVATE && msg.getReceiverString() == userName)) {
                return msg;
            }
        } else {
            return null;
        }
        return null;
    }

    private static void info(String msg, @Nullable Object... params) {
        logger.log(Level.INFO, msg, params);
    }

    private static void error(String msg, @Nullable Object... params) {
        logger.log(Level.WARNING, msg, params);
    }
}
