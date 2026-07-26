package ar.edu.itba.paw.webapp.DTO.Input;

import ar.edu.itba.paw.webapp.validation.ValidPassword;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

// Nota: Asegúrate de tener las dependencias de JSR-303 (Hibernate Validator) en tu pom.xml
@XmlRootElement
public class RegisterUserDTO {

    @NotEmpty(message = "{signUp.name.empty}")
    @Size(min = 1, max = 100, message = "{signUp.name.size}")
    private String name;

    @Email(message = "{signUp.email.invalid}")
    @NotEmpty(message = "{signUp.email.empty}")
    @Size(min = 6, max = 100, message = "{signUp.email.size}")
    private String email;

    @NotEmpty(message = "{signUp.password.empty}")
    @Size(min = 8, message = "{signUp.password.size}")
    @ValidPassword
    private String password;

    // Opcional: para permitir que el cliente envíe el código de idioma (ej: "es", "en")
    private String locale;

    // Constructor vacío requerido por JAX-RS/Jackson
    public RegisterUserDTO() {}

    // Getters y Setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }
}
