package be.msec.labgrpc.server;

import be.msec.labgrpc.User;
import be.msec.labgrpc.exceptions.DuplicateUsernameException;
import be.msec.labgrpc.exceptions.UserNotFoundException;

import java.util.*;

public class UserManager {
    private final List<Message> messages;
    private final Map<String, User> users;

    public UserManager() {
        messages = new ArrayList<>();
        users = new HashMap<>();
    }

    public void connectUser(String username, Object mutex) throws DuplicateUsernameException {
        synchronized (mutex) {
            if (users.containsKey(username)) {
                throw new DuplicateUsernameException(username);
            } else {
                User user = new User(username);
                users.put(username, user);
                mutex.notifyAll();
            }
        }
    }

    public void disconnectUser(String username, Object mutex) throws UserNotFoundException {
        synchronized (mutex) {
            if (users.containsKey(username)) {
                users.remove(username);
            } else {
                throw new UserNotFoundException("Could not find user: " + username);
            }
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
            boolean isBroadcast = msg.getType() == MessageType.BROADCAST;
            boolean isPrivate = msg.getType() == MessageType.PRIVATE;
            if (isBroadcast) {
                return msg;
            } else {
                boolean isCorrectUser = msg.getReceiverString().contains(userName) || msg.getSender().toString().contains(userName);
                if (isCorrectUser) {
                    return msg;
                }
            }
        } else {
            return null;
        }
        return null;
    }

    public List<String> getOnlineUsers() {
        Set<String> set = users.keySet();
        return new ArrayList<>(set);
    }
}
