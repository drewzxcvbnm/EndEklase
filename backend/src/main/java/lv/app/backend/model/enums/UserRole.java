package lv.app.backend.model.enums;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum UserRole {
    ADMIN("ADMIN"),
    USER("USEr");

    private final String role;

    public String role() {
        return role;
    }
}