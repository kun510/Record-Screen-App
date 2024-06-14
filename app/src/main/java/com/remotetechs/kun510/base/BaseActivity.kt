package com.remotetechs.kun510.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setUpBinding()
        setupUI()
        setupListen()
        setupViewModel()
        observeViewModel()
    }

    open fun setUpBinding() {
        //Binding
    }

    open fun setupUI() {
        //MappingUI
    }

    open fun setupListen() {
        //handle
    }

    open fun setupViewModel() {
        //setupViewModel
    }

    open fun observeViewModel() {
        //observeViewModel
    }
}
