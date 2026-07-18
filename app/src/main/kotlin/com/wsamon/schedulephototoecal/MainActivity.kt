package com.wsamon.schedulephototoecal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.wsamon.schedulephototoecal.navigation.ScheduleNavGraph
import com.wsamon.schedulephototoecal.ui.theme.SchedulePhotoToEcalTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ScheduleImportViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SchedulePhotoToEcalTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ScheduleNavGraph(viewModel = viewModel)
                }
            }
        }
    }
}
