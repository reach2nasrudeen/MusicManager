package ir.heydarii.musicmanager.base.di

import dagger.Component
import ir.heydarii.musicmanager.repository.dbinteractor.AlbumsDAO
import ir.heydarii.musicmanager.retrofit.RetrofitMainInterface
import ir.heydarii.musicmanager.utils.AppDatabase
import retrofit2.Retrofit
import javax.inject.Singleton

@Singleton
@Component(modules = [RetrofitModule::class, RoomModule::class])
interface RetrofitComponent {

    fun getRetrofit(): Retrofit

    fun getRetrofitMainInterface(): RetrofitMainInterface

    fun getAppDataBase(): AppDatabase

    fun getAlbumsDAO(): AlbumsDAO

}