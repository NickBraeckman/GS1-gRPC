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

    public void connectUser(String name) throws DuplicateUsernameException {
        if (users.containsKey(name)) {
            throw new DuplicateUsernameException(name);
        } else {
            User user = new User(name);
            users.put(name, user);
        }
    }

    public User findUserByName(String name) throws UserNotFoundException {
        User u = users.get(name);
        if (u != null) {
            return u;
        } else throw new UserNotFoundException(name);
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
