package com.example.possystem.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.possystem.data.entity.UserEntity;

import java.util.List;

@Dao
public interface UserDao {

    @Insert
    long insertUser(UserEntity user);

    @Query("SELECT * FROM users WHERE username = :username AND password = :password LIMIT 1")
    UserEntity authenticate(String username, String password);

    @Query("SELECT * FROM users ORDER BY username ASC")
    List<UserEntity> getAllUsers();
}
