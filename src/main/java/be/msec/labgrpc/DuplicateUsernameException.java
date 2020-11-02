package be.msec.labgrpc;

public class DuplicateUsernameException extends Exception{
    public DuplicateUsernameException(String message) {super(message);}
}
