package com.haekitchenapp.recipeapp.utility;

import com.haekitchenapp.recipeapp.entity.User;
import com.haekitchenapp.recipeapp.model.request.user.UserRequestDto;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User mapToUser(UserRequestDto userRequestDto) {
        User user = new User();
        user.setEmail(userRequestDto.getEmail().toLowerCase());
        user.setPassword(hashPassword(userRequestDto.getPassword()));
        return user;
    }


    private String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }
}
