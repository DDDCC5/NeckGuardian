package com.example.neckguardian

import com.example.neckguardian.content.GuardianTypography
import com.example.neckguardian.content.HomeVolume
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.foundation.dsl.Stage
import com.pico.spatial.ui.foundation.dsl.DefaultWindowContainer
import com.pico.spatial.ui.foundation.dsl.SpatialAppScope

fun mainApp(scope: SpatialAppScope, state: com.example.neckguardian.content.GuardianState) =
    with(scope) {
        DefaultWindowContainer {
            PicoTheme {
                GuardianTypography { HomeVolume(state) }
            }
        }
        Stage(id = "GuardianTrainingStage") {
            PicoTheme { GuardianTypography { com.example.neckguardian.content.TrainingStage(state) } }
        }
    }
