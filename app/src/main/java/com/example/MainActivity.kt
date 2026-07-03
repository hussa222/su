package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.presentation.CanvasScreen
import com.example.presentation.HomeScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        CanvasAppNavigation()
      }
    }
  }
}

@Composable
fun CanvasAppNavigation() {
  val navController = rememberNavController()

  NavHost(
    navController = navController,
    startDestination = "home",
    modifier = Modifier.fillMaxSize(),
    enterTransition = {
      fadeIn(animationSpec = tween(300)) + slideInHorizontally(
        initialOffsetX = { 300 },
        animationSpec = tween(300)
      )
    },
    exitTransition = {
      fadeOut(animationSpec = tween(300)) + slideOutHorizontally(
        targetOffsetX = { -300 },
        animationSpec = tween(300)
      )
    },
    popEnterTransition = {
      fadeIn(animationSpec = tween(300)) + slideInHorizontally(
        initialOffsetX = { -300 },
        animationSpec = tween(300)
      )
    },
    popExitTransition = {
      fadeOut(animationSpec = tween(300)) + slideOutHorizontally(
        targetOffsetX = { 300 },
        animationSpec = tween(300)
      )
    }
  ) {
    composable("home") {
      HomeScreen(
        onCreateNewProject = {
          navController.navigate("canvas")
        }
      )
    }

    composable("canvas") {
      CanvasScreen(
        onNavigateBack = {
          navController.popBackStack()
        }
      )
    }
  }
}
