package ar.edu.itba.paw.interfaces.exceptions;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message);
    }

    @Override
    public String getLocalizedMessage() {
        return "error.user.notFound";
    }
}
