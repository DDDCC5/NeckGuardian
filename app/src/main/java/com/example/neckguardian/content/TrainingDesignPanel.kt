package com.example.neckguardian.content

import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.background
import com.example.neckguardian.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.neckguardian.domain.oneDecimal
import com.pico.spatial.ui.design.*
import kotlin.math.*

/** Accepted 800 × 600 training board. The central opening is occupied by real 3D content. */
@Composable
fun TrainingDesignPanel(s:GuardianState,kind:String,failed:Boolean,onLeave:()->Unit) {
    s.tick
    // design-style: design-surface training-world-controls
    Column(Modifier.size(800.dp,600.dp).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
      // design-style: design-surface warm-training-hud
      Column(Modifier.fillMaxWidth().background(GuardianColors.health,RoundedCornerShape(24.dp)).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        Image(painterResource(when(kind){"tower"->R.drawable.ic_tower;"flight"->R.drawable.ic_glide;"extension"->R.drawable.ic_extension;else->R.drawable.ic_lateral}),null,Modifier.size(36.dp))
        Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
        Text(when(kind){"tower"->s.text("稳姿高塔","Posture Tower");"flight"->s.text("舒颈飞行","Gentle Glide");"extension"->s.text("后仰角度测试","Neck extension check");else->s.text("侧伸角度测试","Side bend check")},style=PicoTheme.typography.titleLarge,color=GuardianColors.ink,fontWeight=FontWeight.Bold)
        Text(when(kind){"tower"->s.text("舒适坐稳，轻轻回正","Sit comfortably and gently return upright");"flight"->s.text("缓慢侧倾，沿着星光飞行","Lean slowly and follow the stars");"extension"->s.text("先笔直坐稳，再缓慢向后仰头","Sit upright, then slowly look upward");else->s.text("保持肩部放松，缓慢向左或向右侧倾","Relax your shoulders and slowly bend to either side")},color=GuardianColors.ink)
        }
        }
        TrainingStats(s,kind)
      }
        // The 3D model sits in front of this region, never behind the metric cards or actions.
        Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.BottomCenter) {
            val hint=when {
                failed -> s.text("场景加载失败，请返回后重试。","Scene loading failed. Return and try again.")
                s.topper>0 -> s.text("高塔倾倒了，正在保存记录","The tower has toppled. Saving your session")
                s.countdown>0 -> s.text("准备：${s.countdown}","Ready: ${s.countdown}")
                !s.pose.fresh -> s.text("正在等待头显追踪…","Waiting for headset tracking…")
                !s.active && kind=="tower" -> s.text("注视高塔，捏合并拖到桌面；松手后校准开始","Look at the tower, pinch and move it onto your desk, then release and calibrate")
                !s.active -> s.text("目视前方，保持自然坐姿，校准后开始","Look straight ahead in a natural seated posture, then calibrate to begin")
                kind=="tower" -> s.text("随倾角轻摆，回正即恢复竖直","The tower leans with you and restores when you return upright")
                kind=="flight" -> s.text("慢慢向左或向右，保持动作舒适","Move slowly left or right and stay comfortable")
                else -> s.text("如有不适，请停止；刻度不代表目标角度。","Stop if you feel discomfort. The scale is not a target.")
            }
            if(kind=="flight" && s.active) Box(Modifier.align(Alignment.Center).offset(y=(-24).dp)) {CollectionFeedback(s)}
            if(kind=="extension") {
                Column(Modifier.align(Alignment.Center).offset(x=60.dp).height(240.dp),verticalArrangement=Arrangement.SpaceBetween) {
                    listOf("60°","45°","30°","15°","0°").forEach { Text(it,color=GuardianColors.sageText) }
                }
            }
            // design-style: design-surface warm-training-hint
            Box(Modifier.fillMaxWidth().background(GuardianColors.health,RoundedCornerShape(16.dp)).padding(12.dp),contentAlignment=Alignment.Center) {
                Text(hint,style=PicoTheme.typography.bodySmall,color=GuardianColors.ink)
            }
            if(kind=="flight" && s.active && s.showQuickStop) {
                Box(Modifier.align(Alignment.TopCenter).padding(top=12.dp)) {
                    DesignStageButton(s.text("停止飞行","Stop flight"),true,{s.finish()})
                }
            }
        }
        if(s.message.isNotBlank()) Text(s.message,color=GuardianColors.ink)
        // design-style: design-surface warm-training-actions
        Row(Modifier.fillMaxWidth().background(GuardianColors.health,RoundedCornerShape(24.dp)).padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            if(kind=="tower") Text(s.text("角度上限 ${s.limit}°","Limit ${s.limit}°"),style=PicoTheme.typography.bodySmall,color=GuardianColors.ink)
            Spacer(Modifier.weight(1f))
            if(!s.active && s.countdown==0 && s.topper==0.0) {
                DesignStageButton(s.text("返回选择","Back to selection"),false,{s.nav(if(kind in listOf("tower","flight")) "trainingSelect" else "movementSelect");onLeave()})
                DesignStageButton(s.text("重新放到正前方","Recenter in front"),false,{s.recenterRevision++})
                DesignStageButton(if(kind in listOf("tower","flight")) s.text("进入训练挑战","Enter training challenge") else s.text("进入常规测试","Enter movement check"),true,{s.start(kind)},s.pose.fresh && s.modelReady && !failed)
            } else {
                if(kind in listOf("extension","lateral") && s.active) DesignStageButton(s.text("校准坐姿","Calibrate posture"),false,{s.pose.calibrate()})
                DesignStageButton(when(kind){"tower"->s.text("终止挑战","End challenge");"flight"->s.text("停止训练","Stop training");else->s.text("结束并记录","Finish and save")},true,{s.finish()},s.topper==0.0)
            }
        }
    }
}

@Composable
private fun TrainingStats(s:GuardianState,kind:String) {
    fun live(value:Double)=if(s.pose.fresh) "${oneDecimal(value)}°" else "—"
    val stats=when(kind) {
        "tower"->listOf(s.text("当前低头角度","Downward angle") to live(max(0.0,-s.pose.pitch)),s.text("挑战剩余","Time remaining") to trainingClock((s.tower?.targetSeconds?:s.minutes*60.0)-(s.tower?.elapsedSeconds?:0.0)),s.text("本次宽容剩余","Allowance remaining") to if((s.tower?.episodeOverSeconds?:0.0)>0) "${oneDecimal((s.grace-(s.tower?.episodeOverSeconds?:0.0)).coerceAtLeast(0.0))} ${s.text("秒","s")}" else "—")
        "flight"->listOf(s.text("训练时长","Duration") to trainingClock(s.elapsed),s.text("已收集星星","Stars collected") to "${s.glide?.stars?:0}",s.text("错过星星","Stars missed") to "${s.glide?.missed?:0}",s.text("完成率","Completion rate") to if((s.glide?.resolved?:0)>0) "${oneDecimal((s.glide?.completion?:0.0)*100)}%" else "—",s.text("当前侧倾","Current side bend") to if(s.pose.fresh) "${if(s.pose.roll<0) s.text("左","Left ") else s.text("右","Right ")}${oneDecimal(abs(s.pose.roll))}°" else "—")
        "extension"->listOf(s.text("当前后仰角度","Current extension") to live(s.pose.pitch.coerceAtLeast(0.0)),s.text("本次最大后仰","Maximum extension") to "${oneDecimal(s.movement.extensionMax)}°",s.text("校准正中姿态","Calibrated neutral") to "0°")
        else->listOf(s.text("左侧最大","Left maximum") to "${oneDecimal(s.movement.leftMax)}°",s.text("正中姿态","Neutral posture") to live(s.pose.roll),s.text("右侧最大","Right maximum") to "${oneDecimal(s.movement.rightMax)}°")
    }
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
        stats.forEach { (label,value) ->
            Column(Modifier.weight(1f).heightIn(min=62.dp).padding(horizontal=4.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(6.dp,Alignment.CenterVertically)) {
                Text(label,style=PicoTheme.typography.bodySmall,color=GuardianColors.ink)
                Text(value,color=GuardianColors.sageText,style=PicoTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
            }
        }
    }
}
@Composable private fun DesignStageButton(label:String,primary:Boolean,onClick:()->Unit,enabled:Boolean=true) {
    Button(onClick=onClick,enabled=enabled,colors=ButtonDefaults.buttonColors(containerColor=if(primary) GuardianColors.primary else GuardianColors.onPrimary,contentColor=if(primary) GuardianColors.onPrimary else GuardianColors.ink)) {Text(label,style=PicoTheme.typography.labelLarge)}
}
private fun trainingClock(seconds:Double):String {val n=seconds.coerceAtLeast(0.0).toInt();return String.format(java.util.Locale.ROOT,"%02d:%02d",n/60,n%60)}
