package be.msec.labgrpc.exceptions;

public class DuplicateUsernameException extends Exception{
    public DuplicateUsernameException(String message) {super(message);}
}
