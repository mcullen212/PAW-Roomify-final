package ar.edu.itba.paw.interfaces.exceptions;

public class ContactNotFoundException extends RuntimeException {
    public ContactNotFoundException(long contactId) {
        super("Contact not found with id: " + contactId);
    }

    @Override
    public String getLocalizedMessage() {
        return "error.contact.notFound";
    }
}
