package com.example.neckguardian.content

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.neckguardian.R
import com.pico.spatial.ui.design.*
import kotlinx.coroutines.delay
import kotlin.math.*

/** App-owned, non-interactive illustrations. Values come from real session results only. */
@Composable
internal fun CompletionRing(value:Double?,label:String,icon:Int,modifier:Modifier=Modifier) {
    var reveal by remember(value) {mutableStateOf(false)}
    LaunchedEffect(value) {reveal=true}
    val sweep by animateFloatAsState(if(reveal) (value?:0.0).coerceIn(0.0,1.0).toFloat() else 0f,tween(650),label="completionReveal")
    Box(modifier.size(160.dp),contentAlignment=Alignment.Center) {
        Canvas(Modifier.fillMaxSize().clearAndSetSemantics {}) {
            val inset=12.dp.toPx();val extent=Size(size.width-inset*2,size.height-inset*2)
            drawArc(GuardianColors.selected,-90f,360f,false,Offset(inset,inset),extent,style=Stroke(9.dp.toPx(),cap=StrokeCap.Round))
            drawArc(GuardianColors.primary,-90f,360f*sweep,false,Offset(inset,inset),extent,style=Stroke(9.dp.toPx(),cap=StrokeCap.Round))
        }
        Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Image(painterResource(icon),null,Modifier.size(28.dp))
            Text(if(value==null) "—" else "${(value.coerceIn(0.0,1.0)*100).roundToInt()}%",style=PicoTheme.typography.titleLarge,fontWeight=FontWeight.Bold,color=GuardianColors.sageText)
            Text(label,style=PicoTheme.typography.bodySmall,color=GuardianColors.ink)
        }
    }
}

/** A transparent attachment on the real tower's placement root, including while dragging. */
@Composable
internal fun StabilityHalo(s:GuardianState) {
    s.tick
    if(!s.pose.fresh) return
    val over=s.active && (s.tower?.episodeOverSeconds?:0.0)>0
    val color=if(over) GuardianColors.warning else GuardianColors.primary
    Canvas(Modifier.size(180.dp).clearAndSetSemantics {}) {
        val inset=14.dp.toPx();val extent=Size(size.width-inset*2,size.height-inset*2)
        drawOval(color.copy(alpha=.045f),Offset(inset,inset),extent)
        drawOval(color.copy(alpha=.18f),Offset(inset,inset),extent,style=Stroke(3.dp.toPx()))
        drawArc(color.copy(alpha=.48f),-90f,if(over) 360f*(1-((s.tower?.episodeOverSeconds?:0.0)/s.grace).coerceIn(0.0,1.0)).toFloat() else 360f,false,Offset(inset,inset),extent,style=Stroke(1.dp.toPx(),cap=StrokeCap.Round))
    }
}

/** Quiet flight wake on a transparent plane below the authored aircraft; no score mutation. */
@Composable
internal fun FlightWake() {
    Canvas(Modifier.size(420.dp,240.dp).clearAndSetSemantics {}) {
        val x=size.width*.5f
        for(side in listOf(-1,1)) {
            val path=Path().apply {moveTo(x+side*size.width*.065f,size.height*.08f);cubicTo(x+side*size.width*.09f,size.height*.35f,x+side*size.width*.21f,size.height*.68f,x+side*size.width*.26f,size.height*.94f)}
            drawPath(path,GuardianColors.primary.copy(alpha=.16f),style=Stroke(1.dp.toPx(),cap=StrokeCap.Round))
        }
    }
}

@Composable
internal fun CollectionFeedback(s:GuardianState) {
    s.tick
    val count=s.glide?.stars?:0
    var show by remember {mutableStateOf(false)}
    var previous by remember {mutableIntStateOf(count)}
    LaunchedEffect(count) {
        if(count>previous) {previous=count;show=true;delay(650);show=false}
        else previous=count
    }
    val fade by animateFloatAsState(if(show) 1f else 0f,tween(180),label="starPickupFade")
    if(fade>.01f) Column(Modifier.graphicsLayer {alpha=fade;translationY=-8.dp.toPx()*(1-fade)},horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(4.dp)) {
        Image(painterResource(R.drawable.ic_glide),null,Modifier.size(32.dp))
        Text("+1",style=PicoTheme.typography.titleLarge,fontWeight=FontWeight.Bold,color=GuardianColors.sageText)
    }
}
