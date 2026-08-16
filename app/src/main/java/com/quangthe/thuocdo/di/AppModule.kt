package com.quangthe.thuocdo.di

import android.content.Context
import com.quangthe.thuocdo.data.RulerRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideRulerRepository(@ApplicationContext context: Context): RulerRepository {
        return RulerRepository(context)
    }
}
