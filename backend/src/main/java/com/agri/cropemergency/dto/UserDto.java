package com.agri.cropemergency.dto;

import com.agri.cropemergency.model.User;

public record UserDto(Long id, String name, String email, String phone, String language, String role) {
    public static UserDto from(User u) {
        return new UserDto(u.getId(), u.getName(), u.getEmail(), u.getPhone(), u.getLanguage(), u.getRole().name());
    }
}
