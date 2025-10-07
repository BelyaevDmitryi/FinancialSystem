package com.fs.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.fs.domain.User;
import com.fs.dto.PositionsDto;
import com.fs.dto.UserDtoCreate;
import com.fs.service.UserService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    @PostMapping
    public User createUser(@RequestBody UserDtoCreate userDtoCreate) {
        return userService.createUser(userDtoCreate);
    }

    @GetMapping("/{id}")
    public User getUserById(@PathVariable String id) {
        return userService.getUserById(id);
    }

    @PutMapping("/{id}/stocks")
    public User addStocksToUser(@PathVariable String id, @RequestBody PositionsDto positionsDto) {
        return userService.addStocksToUser(id, positionsDto);
    }

    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable String id) {
        userService.deleteUserById(id);
    }

    @GetMapping
    public List<User> getUsers() {
        return userService.getAllUsers();
    }

}
