package com.example.neckguardian.content

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.pico.spatial.ui.design.LocalTextStyle
import com.pico.spatial.ui.design.PicoTheme

/** Shared 14sp reading baseline. Native components retain their own slot/state behavior. */
@Composable
fun GuardianTypography(content:@Composable ()->Unit) {
    CompositionLocalProvider(LocalTextStyle.provides(PicoTheme.typography.bodyMedium),content=content)
}
