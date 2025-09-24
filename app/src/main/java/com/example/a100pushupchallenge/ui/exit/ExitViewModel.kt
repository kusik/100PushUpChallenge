package com.example.a100pushupchallenge.ui.exit

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ExitViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is Exit Fragment"
    }
    val text: LiveData<String> = _text
}