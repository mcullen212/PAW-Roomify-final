package ar.edu.itba.paw.webapp.DTO;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ResponseMessageDTO {

    private String message;

    public ResponseMessageDTO() {
        // Required by JAX-RS
    }

    public ResponseMessageDTO(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}