package com.example.possystem.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.possystem.data.entity.ReturnEntity;
import com.example.possystem.data.entity.ReturnItemEntity;

import java.util.List;

@Dao
public interface ReturnDao {

    @Insert
    long insertReturn(ReturnEntity returnEntity);

    @Insert
    void insertReturnItems(List<ReturnItemEntity> items);

    @Query("SELECT * FROM returns ORDER BY timestamp DESC")
    List<ReturnEntity> getAllReturns();

    @Query("SELECT * FROM return_items WHERE returnId = :returnId")
    List<ReturnItemEntity> getItemsForReturn(int returnId);
}
