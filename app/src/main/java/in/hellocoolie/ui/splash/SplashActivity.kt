package in.hellocoolie.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import in.hellocoolie.data.repository.AuthRepository
import in.hellocoolie.ui.auth.AuthActivity
import in.hellocoolie.ui.porter.PorterMainActivity
import in.hellocoolie.ui.user.UserMainActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    @Inject lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // No layout — pure splash with theme window background

        lifecycleScope.launch {
            delay(1800) // Show splash for 1.8s
            navigate()
        }
    }

    private fun navigate() {
        val intent = when {
            !authRepository.isLoggedIn() -> Intent(this, AuthActivity::class.java)
            authRepository.getRole() == "porter" -> Intent(this, PorterMainActivity::class.java)
            else -> Intent(this, UserMainActivity::class.java)
        }
        startActivity(intent)
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}
