package be.msec.labgrpc.exceptions;

import java.util.logging.Level;
import java.util.logging.Logger;

public class DuplicateUsernameException extends Exception {
    private static final Logger logger = Logger.getLogger(DuplicateUsernameException.class.getName());

    public DuplicateUsernameException(String message) {
        super(message);
        logger.log(Level.SEVERE, "Duplicate user name entered," + message);
    }
}
