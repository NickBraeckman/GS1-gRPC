package be.msec.labgrpc.exceptions;

import java.util.logging.Level;
import java.util.logging.Logger;

public class UserNotFoundException extends Exception {
    private static final Logger logger = Logger.getLogger(UserNotFoundException.class.getName());

    public UserNotFoundException(String msg) {
        super(msg);
        logger.log(Level.SEVERE, "Could not find user");
    }
}
