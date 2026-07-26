package ar.edu.itba.paw.webapp.DTO.Output;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ApiErrorDTO {

    // El código de estado HTTP (ej: 409, 401)
    private int status;

    // Un mensaje de error descriptivo para el cliente
    private String message;

    public ApiErrorDTO() {
        // Required by JAX-RS
    }

    public ApiErrorDTO(int status, String message) {
        this.status = status;
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}