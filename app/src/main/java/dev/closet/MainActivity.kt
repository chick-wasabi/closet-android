package dev.closet

import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.closet.ui.ClosetApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash=installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setOnExitAnimationListener { view ->
            if(android.animation.ValueAnimator.areAnimatorsEnabled()) view.view.animate().alpha(0f).setDuration(160).withEndAction{view.remove()}.start()
            else view.remove()
        }
        enableEdgeToEdge()
        setContent { ClosetApp(viewModel()) }
    }
}
