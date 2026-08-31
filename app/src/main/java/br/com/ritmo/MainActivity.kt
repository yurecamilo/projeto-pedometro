package br.com.ritmo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.ritmo.data.ActivityDatabase
import br.com.ritmo.ui.RitmoApp

class MainActivity : ComponentActivity() {
    private val viewModel: PedometerViewModel by viewModels {
        viewModelFactory { initializer {
            PedometerViewModel(this@MainActivity.application, ActivityDatabase.create(this@MainActivity.application).activityDao())
        } }
    }
    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.startTracking()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            val week by viewModel.week.collectAsStateWithLifecycle()
            RitmoApp(state, week, viewModel::updateProfile)
        }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
            viewModel.startTracking()
        } else permission.launch(Manifest.permission.ACTIVITY_RECOGNITION)
    }

    override fun onPause() { viewModel.stopTracking(); super.onPause() }
}
