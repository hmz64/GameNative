package com.quizgame.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizgame.data.model.JoinRoomResponse
import com.quizgame.data.model.Room
import com.quizgame.data.repository.GameRepository
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val repository = GameRepository()

    private val _createRoomResult = MutableLiveData<Result<Room>>()
    val createRoomResult: LiveData<Result<Room>> = _createRoomResult

    private val _joinRoomResult = MutableLiveData<Result<JoinRoomResponse>>()
    val joinRoomResult: LiveData<Result<JoinRoomResponse>> = _joinRoomResult

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    fun createRoom(hostName: String) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.createRoom(hostName)
            _createRoomResult.value = result
            _isLoading.value = false
        }
    }

    fun joinRoom(roomCode: String, username: String) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.joinRoom(roomCode, username)
            _joinRoomResult.value = result
            _isLoading.value = false
        }
    }
}
