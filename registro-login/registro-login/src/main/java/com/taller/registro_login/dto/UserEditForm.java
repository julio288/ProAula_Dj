package com.taller.registro_login.dto;

import com.taller.registro_login.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserEditForm {

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Debe indicar un correo válido")
    @Size(max = 100)
    private String email;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 50)
    private String firstName;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(min = 2, max = 50)
    private String lastName;

    public static UserEditForm from(User user) {
        UserEditForm form = new UserEditForm();
        form.setEmail(user.getEmail());
        form.setFirstName(user.getFirstName());
        form.setLastName(user.getLastName());
        return form;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
}