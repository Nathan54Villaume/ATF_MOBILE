package com.riva.atsmobile.data.local

import androidx.room.*

@Dao
interface EtapesDao {
    @Query("SELECT * FROM ETAPES_CHANG_GAMMES")
    suspend fun getAll(): List<EtapeEntity>

    @Query("SELECT * FROM ETAPES_CHANG_GAMMES WHERE idEtape = :id")
    suspend fun getById(id: Int): EtapeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(steps: List<EtapeEntity>)

    @Update
    suspend fun update(step: EtapeEntity)

    @Delete
    suspend fun delete(step: EtapeEntity)
}
