package com.hellocoolie.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import com.hellocoolie.R
import com.hellocoolie.data.repository.AuthRepository
import com.hellocoolie.ui.auth.AuthActivity
import com.hellocoolie.ui.porter.PorterMainActivity
import com.hellocoolie.ui.user.UserMainActivity
import javax.inject.Inject

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    @Inject lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Use Handler - no coroutine overhead, fires immediately after layout renders
        Handler(Looper.getMainLooper()).postDelayed({
            navigate()
        }, 1500)
    }

    private fun navigate() {
        val intent = when {
            !authRepository.isLoggedIn()         -> Intent(this, AuthActivity::class.java)
            authRepository.getRole() == "porter" -> Intent(this, PorterMainActivity::class.java)
            else                                 -> Intent(this, UserMainActivity::class.java)
        }
        startActivity(intent)
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}
