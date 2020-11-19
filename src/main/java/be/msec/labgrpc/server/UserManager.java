package be.msec.labgrpc.server;

import be.msec.labgrpc.User;
import be.msec.labgrpc.exceptions.DuplicateUsernameException;
import be.msec.labgrpc.exceptions.UserNotFoundException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserManager {
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

    // add message to list and notify the synchronization method
    public void addToMessages(Message message, Object mutex) {
        synchronized (mutex) {
            try {
                messages.add(message);
                mutex.notifyAll();
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
}
