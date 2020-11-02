package be.msec.labgrpc.server;

import be.msec.labgrpc.DuplicateUsernameException;
import be.msec.labgrpc.Message;
import be.msec.labgrpc.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatServerController {
    private final List<Message> messages;
    private final Map<String, User> users;

    public ChatServerController() {
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
            } catch (Exception e){
                e.printStackTrace();
            }
        }

    }

    public Message getLastMessage() {
        return messages.get(messages.size() - 1);
    }
}
