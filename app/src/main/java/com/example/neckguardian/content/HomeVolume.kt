package com.example.neckguardian.content

import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.neckguardian.R
import com.example.neckguardian.platform.SessionRecord
import com.pico.spatial.ui.design.*
import com.pico.spatial.ui.design.windows.AlertDialog
import com.pico.spatial.ui.platform.containers.LocalSpatialNavigator
import com.pico.spatial.ui.platform.containers.StageStyle
import com.pico.spatial.ui.platform.containers.OpenStageResult
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date
import kotlin.math.*

// Approved warm card colors, owned by the application; native PicoTheme stays unchanged.
object GuardianColors {
    // design-style: fixed-figma-color production/ui/theme.json approved warm palette
    val health=Color(0xFFF5F0E4) // design-style: fixed-figma-color production/ui/theme.json
    // design-style: fixed-figma-color production/ui/theme.json approved warm palette
    val tower=Color(0xFFF4E9DF) // design-style: fixed-figma-color production/ui/theme.json
    // design-style: fixed-figma-color production/ui/theme.json approved warm palette
    val flight=Color(0xFFE9EEE2) // design-style: fixed-figma-color production/ui/theme.json
    // design-style: fixed-figma-color production/ui/theme.json approved warm palette
    val ink=Color(0xFF354B40) // design-style: fixed-figma-color production/ui/theme.json
    val primary=Color(0xFF557665) // design-style: fixed-figma-color production/ui/theme.json
    val onPrimary=Color(0xFFFFFAF0) // design-style: fixed-figma-color production/ui/theme.json
    val selected=Color(0xFFD5E1D0) // design-style: fixed-figma-color .scratch/design-spec.json selected
    val selectedText=Color(0xFF365B42) // design-style: fixed-figma-color .scratch/design-spec.json selectedText
    val sageText=Color(0xFF527360) // design-style: fixed-figma-color .scratch/design-spec.json sageText
    val note=Color(0xFFEEE4CC) // design-style: fixed-figma-color .scratch/design-spec.json note
    val resultMark=Color(0xFFDFE8D8) // design-style: fixed-figma-color .scratch/design-spec.json resultMark
    val historyGreen=Color(0xFF9FB299) // design-style: fixed-figma-color .scratch/design-spec.json historyGreen
    val historyPeach=Color(0xFFD7AA8C) // design-style: fixed-figma-color .scratch/design-spec.json historyPeach
    val historyGold=Color(0xFFDFC58E) // design-style: fixed-figma-color .scratch/design-spec.json historyGold
}

@Composable
fun HomeVolume(state: GuardianState) {
    val context=LocalContext.current
    val navigator=LocalSpatialNavigator.current
    val scope=rememberCoroutineScope()
    val locale=LocalConfiguration.current.locales[0]
    state.zh=locale.language=="zh"
    val owner=LocalLifecycleOwner.current
    DisposableEffect(owner) {

        val observer=LifecycleEventObserver { _,e ->
            when(e) {
                Lifecycle.Event.ON_START -> Unit
                Lifecycle.Event.ON_STOP -> if(!state.inStage) { if(state.active || state.countdown>0) state.finish("background");state.pose.stop();state.audio.stop() }
                Lifecycle.Event.ON_DESTROY -> {ConsentSession.accepted=false;state.active=false;state.audio.stop()}
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose {owner.lifecycle.removeObserver(observer);if(!state.inStage) {state.pose.stop();state.audio.stop();ConsentSession.accepted=false}}
    }
    LaunchedEffect(Unit) { delay(900);state.page=if(ConsentSession.accepted) "home" else "disclaimer" }
    LaunchedEffect(Unit) {
        var last=SystemClock.elapsedRealtime()
        while(true) {delay(50);val now=SystemClock.elapsedRealtime();if(!state.inStage) state.step((now-last)/1000.0);last=now}
    }
    val t=state::text
    val restoredPage=state.page in listOf("towerSetup","results","healthResult","history")
    Column(Modifier.fillMaxSize().padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        if(state.page!="splash") TitleBar(
            title={Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
              if(state.page in listOf("home","disclaimer")) Image(painterResource(R.drawable.app_icon),null,Modifier.size(28.dp))
              Column {
                Text(when(state.page) {
                    "towerSetup" -> t("稳姿高塔 · 训练设置","Posture Tower · Setup")
                    "results" -> t("训练结果","Training results")
                    "healthResult" -> t("自评结果","Self-check summary")
                    else -> title(if(state.page=="disclaimer") "home" else state.page,state.zh)
                },style=PicoTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                if(state.page in listOf("towerSetup","history")) Text(when(state.page) {
                    "towerSetup" -> t("保持舒适坐姿，让积木稳稳站立","Keep a comfortable posture and the blocks balanced")
                    else -> t("最近 30 条记录","Latest 30 records")
                },style=PicoTheme.typography.bodySmall,color=PicoTheme.colorScheme.labelSecondary)
            }}},
            leadingActions={if(!restoredPage && state.page !in listOf("home","disclaimer") && !state.active && state.countdown==0 && state.topper==0.0) Button(onClick={state.nav("home")}) {Text(t("首页","Home"))}}
        )
        when(if(state.page=="disclaimer") "home" else state.page) {
            "splash" -> Column(Modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
                Image(painterResource(R.drawable.app_icon),null,Modifier.size(128.dp));Text(t("颈椎卫士","NeckGuardian"));Text(t("放松肩颈，找回舒适姿态","Relax your shoulders. Find your comfortable posture."))
            }
            "home" -> {
                Row(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                    // design-style: design-surface home-health
                    Entry(t("颈椎健康测试","Neck health check"),t("六道问题，了解近期感受","Six questions about how you feel"),R.drawable.ic_health_check,Modifier.weight(1f).background(GuardianColors.health,RoundedCornerShape(20.dp))) {state.answers=List(6){-1};state.question=0;state.nav("questionnaire")}
                    // design-style: design-surface home-training
                    Entry(t("颈椎挑战训练","Training challenges"),t("稳姿与舒缓侧伸","Posture and gentle side bends"),R.drawable.ic_training,Modifier.weight(1f).background(GuardianColors.tower,RoundedCornerShape(20.dp))) {state.nav("trainingSelect")}
                    // design-style: design-surface home-routine
                    Entry(t("颈椎常规测试","Movement checks"),t("记录舒适活动角度","Record your comfortable movement"),R.drawable.ic_routine,Modifier.weight(1f).background(GuardianColors.flight,RoundedCornerShape(20.dp))) {state.nav("movementSelect")}
                }
                Action(t("历史记录","History"),"home.history") {state.nav("history")}
            }
            "trainingSelect" -> Row(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                // design-style: design-surface challenge-tower
                Entry(t("稳姿高塔","Posture Tower"),t("让纵向积木随姿态保持平衡","Keep a single tower balanced with your posture"),R.drawable.ic_tower,Modifier.weight(1f).background(GuardianColors.health,RoundedCornerShape(20.dp))) {state.nav("towerSetup")}
                // design-style: design-surface challenge-flight
                Entry(t("舒颈飞行","Gentle Glide"),t("缓缓侧倾，收集沿途星星","Lean slowly and collect stars"),R.drawable.ic_glide,Modifier.weight(1f).background(GuardianColors.flight,RoundedCornerShape(20.dp))) {state.nav("flightSetup")}
            }
            "movementSelect" -> Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text(t("选择一种动作，记录舒适范围内的活动角度","Choose a movement and record your comfortable range"),style=PicoTheme.typography.bodySmall,color=PicoTheme.colorScheme.labelSecondary)
                Row(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                    // design-style: design-surface routine-extension
                    Entry(t("后仰角度测试","Neck extension check"),t("缓慢后仰，查看实时角度与最大记录","Slowly look upward and view your current and maximum angle"),R.drawable.ic_extension,Modifier.weight(1f).background(GuardianColors.health,RoundedCornerShape(20.dp))) {state.nav("extensionSetup")}
                    // design-style: design-surface routine-lateral
                    Entry(t("侧伸角度测试","Side bend check"),t("放松肩部，记录左右两侧的舒适范围","Relax your shoulders and record a comfortable range on each side"),R.drawable.ic_lateral,Modifier.weight(1f).background(GuardianColors.flight,RoundedCornerShape(20.dp))) {state.nav("lateralSetup")}
                }
                Note(state)
            }
            "questionnaire" -> Questionnaire(state)
            "towerSetup","flightSetup","extensionSetup","lateralSetup" -> Setup(state,Modifier.weight(1f)) {
                if(state.inStage) return@Setup
                state.stageKind=state.page.removeSuffix("Setup")
                state.inStage=true
                scope.launch {
                    when(navigator.openStage("GuardianTrainingStage",StageStyle.Mixed)) {
                        is OpenStageResult.Allowed -> {
                            // The Stage minimizes this window after receiving the first real pose.
                            android.util.Log.i("NeckGuardian","Full Space entry allowed: ${state.stageKind}")
                        }
                        else -> { state.inStage=false;state.message=t("无法打开训练空间，请重试。","Could not open the training space. Please try again.") }
                    }
                }
            }
            "tower","flight","extension","lateral" -> Training(state,Modifier.weight(1f))
            "results","healthResult" -> state.result?.let { ResultPanel(state,it,Modifier.weight(1f)) }
            "history" -> HistoryPanel(state,locale,Modifier.weight(1f))
        }
        if(state.message.isNotEmpty()) Text(state.message,color=PicoTheme.colorScheme.labelSecondary)
    }
    if(state.page=="disclaimer") AlertDialog(
        onDismissRequest={},
        title={Text(t("使用前须知","Before you begin"))},
        content={ Column(verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Text(t("本应用提供的颈椎测试与训练内容仅供健康参考","The neck checks and exercises are for wellbeing reference only."))
            Text(t("不构成医疗诊断或治疗建议","They do not constitute medical diagnosis or treatment advice."))
            Text(t("如有颈椎不适或疼痛，请及时就医","If you experience neck discomfort or pain, seek medical care."))
            Text(t("如练习时出现不适，请停止，无需追求更大的角度。","Stop if you feel discomfort. There is no need to aim for a larger angle."))
        } },
        buttons={Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Button(onClick={scope.launch {navigator.closeWindowContainer()}},modifier=Modifier.weight(1f),colors=ButtonDefaults.buttonColors(containerColor=PicoTheme.colorScheme.fillSecondary,contentColor=PicoTheme.colorScheme.labelPrimary)) {Text(t("不同意并退出","Disagree and exit"))}
            Button(onClick={ConsentSession.accepted=true;state.nav("home");state.audio.speak("start",state.zh)},modifier=Modifier.weight(1f)) {Text(t("同意","Agree"))}
        }}
    )
}

@Composable
private fun Action(label:String,id:String,enabled:Boolean=true,onClick:()->Unit) {
    Button(onClick=onClick,enabled=enabled,modifier=Modifier.semantics {testTag=id}) {Text(label,style=PicoTheme.typography.labelLarge)}
}

@Composable
private fun Entry(label:String,description:String,icon:Int,modifier:Modifier,onClick:()->Unit) {
    // design-node: home-module-card (approved bounded module surface)
    Column(modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        val iconSize=if(icon in listOf(R.drawable.ic_health_check,R.drawable.ic_training,R.drawable.ic_routine)) 64.dp else 72.dp
        Image(painterResource(icon),null,Modifier.size(iconSize))
        Text(label,style=PicoTheme.typography.titleMedium,fontWeight=FontWeight.Bold,color=GuardianColors.ink)
        Text(description,style=PicoTheme.typography.bodyMedium,color=GuardianColors.ink,modifier=Modifier.weight(1f))
        Action(label,"entry.$icon",onClick=onClick)
    }
}

@Composable
private fun Questionnaire(s:GuardianState) {
    val cn=listOf("你是否经常感到颈部酸痛或僵硬？","你是否有手麻、手指刺痛的情况？","你是否经常头痛或头晕？","你转头或低头时是否感到活动受限？","你每天低头看手机或电脑超过四小时吗？","你晨起时是否感到颈部僵硬？")
    val en=listOf("Do you feel neck soreness or stiffness?","Do you experience hand numbness or tingling?","Do you experience headaches or dizziness?","Does turning or bending your head feel restricted?","Do you look down at a phone or computer for over four hours a day?","Does your neck feel stiff when you wake up?")
    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Text(s.text("根据日常感受回答，没有标准答案","Answer based on your daily experience. There are no right answers."),color=PicoTheme.colorScheme.labelSecondary)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            Text("${s.question+1} / 6")
            Text(s.text("日常感受自评","Daily wellbeing self-report"))
        }
        LinearProgressIndicator(progress={(s.question+1)/6f},modifier=Modifier.fillMaxWidth())
        // design-node: question-copy
        Text(if(s.zh) cn[s.question] else en[s.question],style=PicoTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
        val choices=listOf("从不" to "Never","偶尔" to "Sometimes","经常" to "Often","总是" to "Always")
        choices.chunked(2).forEachIndexed { row,pairs ->
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                pairs.forEachIndexed { col,p ->
                    val i=row*2+col
                    Option(selected=s.answers[s.question]==i,onSelectChange={selected ->
                        if(selected) {s.answers=s.answers.toMutableList().also{it[s.question]=i};s.audio.effect("button_click")}
                    },modifier=Modifier.weight(1f).heightIn(min=64.dp)) {
                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                            Image(painterResource(if(s.answers[s.question]==i) R.drawable.radio_selected else R.drawable.radio_unselected),null,Modifier.size(24.dp))
                            Text(s.text(p.first,p.second))
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp,Alignment.End)) {
            Action(s.text("返回","Back"),"question.previous") {if(s.question>0) s.question-- else s.nav("home")}
            Action(s.text(if(s.question==5) "查看提示" else "下一题",if(s.question==5) "View reference" else "Next"),"question.next",s.answers[s.question]>=0) {if(s.question==5) s.saveHealth() else s.question++}
        }
    }
}

@Composable
private fun Setup(s:GuardianState,modifier:Modifier,onEnterStage:()->Unit) {
    val kind=s.page.removeSuffix("Setup")
    if(kind=="tower") { TowerSetup(s,modifier,onEnterStage);return }
    Column(modifier,verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Row(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                if(kind=="flight") {
                    Text(s.text("坐稳并放松肩膀。轻轻向左或右侧倾，保持约半秒来转向。","Sit comfortably and relax your shoulders. Gently lean left or right and hold for half a second to steer."))
                    Text(s.text("收集星星；快速抬头可显示停止按钮，也可随时点击下方停止。","Collect stars. A quick upward movement reveals Stop; the standard Stop button is always available."))
                } else Text(s.text(if(kind=="extension") "请笔直坐稳，缓慢后仰，以舒适为限。" else "请笔直坐稳，慢慢向左或向右侧倾，以舒适为限。",if(kind=="extension") "Sit upright. Slowly look upward within your comfortable range." else "Sit upright. Slowly bend to either side within a comfortable range."))
                Text(s.text("开始时请目视前方，保持自然姿势进行零点校准。","Look straight ahead in a natural posture for zero calibration."))
            }
            ScenePanel(s,kind,Modifier.weight(1f).fillMaxHeight())
        }
        SetupActions(s,kind,onEnterStage)
        Note(s)
    }
}

/** One shared footer fixes wording, order, alignment and target sizes across all four setup pages. */
@Composable
private fun SetupActions(s:GuardianState,kind:String,onEnter:()->Unit) {
    val challenge=kind in listOf("tower","flight")
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically) {
        Button(onClick={s.explain(kind)},modifier=Modifier.width(144.dp).heightIn(min=48.dp).semantics {testTag="$kind.guide"},colors=ButtonDefaults.buttonColors(containerColor=PicoTheme.colorScheme.fillSecondary,contentColor=PicoTheme.colorScheme.labelPrimary)) {Text(s.text("重听讲解","Replay guide"),style=PicoTheme.typography.labelLarge)}
        Spacer(Modifier.weight(1f))
        Button(onClick={s.nav(if(challenge) "trainingSelect" else "movementSelect")},modifier=Modifier.width(184.dp).heightIn(min=48.dp).semantics {testTag="$kind.back"},colors=ButtonDefaults.buttonColors(containerColor=PicoTheme.colorScheme.fillSecondary,contentColor=PicoTheme.colorScheme.labelPrimary)) {Text(s.text("返回选择","Back to selection"),style=PicoTheme.typography.labelLarge)}
        Button(onClick=onEnter,enabled=!s.inStage,modifier=Modifier.width(232.dp).heightIn(min=48.dp).semantics {testTag="$kind.enter"},colors=ButtonDefaults.buttonColors(containerColor=GuardianColors.primary,contentColor=GuardianColors.onPrimary)) {
            Text(if(s.inStage) s.text("正在进入…","Entering…") else if(challenge) s.text("进入训练挑战","Enter training challenge") else s.text("进入常规测试","Enter movement check"),style=PicoTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun TowerSetup(s:GuardianState,modifier:Modifier,onEnterStage:()->Unit) {
    Column(modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth().height(120.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text(s.text("稳稳堆叠，轻轻回正","Keep the tower balanced"),style=PicoTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
                    Text(s.text("高塔随低头角度倾斜，回正后恢复。","The tower leans as you look down and restores when you return upright."),style=PicoTheme.typography.bodySmall,color=PicoTheme.colorScheme.labelSecondary)
                }
                ScenePanel(s,"tower",Modifier.width(210.dp).fillMaxHeight())
            }
            Choice(s.text("低头角度上限","Downward tilt limit"),(0..30 step 5).toList(),s.limit,"°") {s.limit=it}
            Choice(s.text("挑战时长","Challenge duration"),(10..60 step 10).toList(),s.minutes,s.text(" 分钟"," min")) {s.minutes=it}
            Choice(s.text("单次宽容时长","Grace period per episode"),(1..10).toList(),s.grace,s.text(" 秒"," s")) {s.grace=it}
            Text(s.text("超限后在宽容时间内回正，不受惩罚。挑战开始后不可暂停，只能终止。","Return within the grace period to continue. Once started, this challenge cannot be paused; it can only be ended."),style=PicoTheme.typography.bodySmall,color=PicoTheme.colorScheme.labelSecondary)
        }
        SetupActions(s,"tower",onEnterStage)
        Note(s)
    }
}

@Composable
private fun Choice(label:String,values:List<Int>,selected:Int,suffix:String,onSelect:(Int)->Unit) {
    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(label,style=PicoTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            values.forEach { v ->
                Button(onClick={onSelect(v)},
                    colors=ButtonDefaults.buttonColors(containerColor=if(v==selected) GuardianColors.selected else PicoTheme.colorScheme.fillSecondary,contentColor=if(v==selected) GuardianColors.selectedText else PicoTheme.colorScheme.labelPrimary),
                    modifier=Modifier.weight(1f).heightIn(min=48.dp).semantics {this.selected=v==selected}) {Text("$v$suffix")}
            }
        }
    }
}

@Composable
private fun Training(s:GuardianState,modifier:Modifier) {
    s.tick
    Row(modifier,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
        Column(Modifier.width(300.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            if(s.countdown>0) Text(s.text("准备：${s.countdown}","Ready: ${s.countdown}"))
            if(s.topper>0) Text(s.text("高塔倾倒了，请稍候","The tower has toppled"))
            when(s.page) {
                "tower" -> s.tower?.let {
                    Text(s.text("当前低头角度","Downward angle")+" ${fmt(max(0.0,-s.pose.pitch))}°")
                    Text(s.text("剩余时间","Time remaining")+" ${clock(it.targetSeconds-it.elapsedSeconds)}")
                    Text(s.text("宽容时间剩余","Allowance remaining")+" ${fmt((s.grace-it.episodeOverSeconds).coerceAtLeast(0.0))} ${s.text("秒","s")}")
                    LinearProgressIndicator(progress={it.completion.toFloat()},modifier=Modifier.fillMaxWidth())
                }
                "flight" -> {Text(s.text("训练时长","Duration")+" ${clock(s.elapsed)}");Text(s.text("已收集星星","Stars collected")+" ${s.glide?.stars?:0}");Text(s.text("错过星星","Stars missed")+" ${s.glide?.missed?:0}");Text(s.text("完成率","Completion rate")+" "+if((s.glide?.resolved?:0)>0) "${fmt((s.glide?.completion?:0.0)*100)}%" else "—");Text(s.text("侧倾角度","Side bend")+" ${fmt(s.pose.roll)}°")}
                "extension" -> {Text(s.text("当前后仰","Current extension")+" ${fmt(s.pose.pitch.coerceAtLeast(0.0))}°");Text(s.text("最大后仰","Maximum extension")+" ${fmt(s.movement.extensionMax)}°")}
                "lateral" -> {Text(s.text("当前侧伸","Current side bend")+" ${fmt(s.pose.roll)}°");Text(s.text("左侧最大","Left maximum")+" ${fmt(s.movement.leftMax)}°");Text(s.text("右侧最大","Right maximum")+" ${fmt(s.movement.rightMax)}°")}
            }
            if(s.showQuickStop && s.active) Action(s.text("停止飞行","Stop flight"),"flight.quickStop") {s.finish()}
            Action(s.text("终止并查看结果","End and view results"),"training.stop",s.active || s.countdown>0) {s.finish()}
            Text(s.text("如感到不适，请立即停止。","Stop immediately if you feel discomfort."),color=PicoTheme.colorScheme.labelSecondary)
        }
        ScenePanel(s,s.page,Modifier.weight(1f).fillMaxHeight())
    }
}

@Composable
private fun PrimaryAction(label:String,id:String,enabled:Boolean=true,onClick:()->Unit) {
    Button(onClick=onClick,enabled=enabled,colors=ButtonDefaults.buttonColors(containerColor=GuardianColors.primary,contentColor=GuardianColors.onPrimary),modifier=Modifier.semantics {testTag=id}) {Text(label,style=PicoTheme.typography.labelLarge)}
}

@Composable
private fun SecondaryAction(label:String,id:String,onClick:()->Unit) {
    Button(onClick=onClick,colors=ButtonDefaults.buttonColors(containerColor=PicoTheme.colorScheme.fillSecondary,contentColor=PicoTheme.colorScheme.labelPrimary),modifier=Modifier.semantics {testTag=id}) {Text(label,style=PicoTheme.typography.labelLarge)}
}

@Composable
private fun ResultMark(health:Boolean,band:Int?) {
    if(health) {
        val icon=when(band) {0->R.drawable.ic_risk_low;1->R.drawable.ic_risk_attention;else->R.drawable.ic_risk_concern}
        // Decorative icon accompanies the explicit localized reference-group headline.
        Image(painterResource(icon),null,Modifier.size(56.dp))
        return
    }
    Box(Modifier.size(48.dp),contentAlignment=Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {drawCircle(GuardianColors.resultMark)}
        Text("✓",style=PicoTheme.typography.titleLarge,color=GuardianColors.sageText)
    }
}

@Composable
private fun ResultPanel(s:GuardianState,r:SessionRecord,modifier:Modifier) {
    val health=r.kind=="health"
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(top=16.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(16.dp)) {
        ResultMark(health,r.metrics["band"]?.toInt())
        // Approved result-title/risk-label hierarchy from production/ui/screens/zh.
        Text(if(health) healthLabel(s,r) else s.text("本次${title(r.kind,true)}已结束","${title(r.kind,false)} ended"),style=PicoTheme.typography.titleLarge,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)
        if(health) {
            Text(s.text("这份问卷不是经过临床验证的诊断量表。结果仅用于提醒你关注自身感受。","This questionnaire is not a clinically validated diagnostic scale. It only supports awareness of your own symptoms."),textAlign=TextAlign.Center,style=PicoTheme.typography.bodyMedium)
            // design-style: design-surface healthResult-reference-note-1
            Box(Modifier.fillMaxWidth().background(GuardianColors.note,RoundedCornerShape(16.dp)).padding(16.dp),contentAlignment=Alignment.Center) {
                Text(s.text("仅作健康参考，不构成医疗诊断","For health reference only. Not a medical diagnosis."),color=GuardianColors.ink,style=PicoTheme.typography.bodyMedium,textAlign=TextAlign.Center)
            }
            Text(s.text("如有颈椎不适或疼痛，请及时就医。","If you have neck discomfort or pain, seek medical care promptly."),color=PicoTheme.colorScheme.labelSecondary,style=PicoTheme.typography.bodyMedium,textAlign=TextAlign.Center)
            PrimaryAction(s.text("返回主菜单","Return home"),"result.home") {s.nav("home")}
        } else {
            val metrics=resultMetrics(s,r)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                // design-style: design-surface result-average
                MetricCard(metrics[0],Modifier.weight(1f).background(PicoTheme.colorScheme.fillSecondary,RoundedCornerShape(18.dp)))
                // design-style: design-surface result-ratio
                MetricCard(metrics[1],Modifier.weight(1f).background(PicoTheme.colorScheme.fillSecondary,RoundedCornerShape(18.dp)))
                // design-style: design-surface result-grace
                if(metrics.size>2) MetricCard(metrics[2],Modifier.weight(1f).background(PicoTheme.colorScheme.fillSecondary,RoundedCornerShape(18.dp)))
            }
            val duration=clock(r.metrics["duration"]?:0.0)
            if(r.kind in listOf("tower","flight")) Text(if(r.kind=="tower") s.text("$duration / ${s.minutes} 分钟","$duration / ${s.minutes} min") else s.text("本次时长 $duration","Session duration $duration"),style=PicoTheme.typography.bodyMedium)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                SecondaryAction(s.text("查看历史","View history"),"result.history") {s.nav("history")}
                PrimaryAction(s.text("返回主菜单","Return home"),"result.home") {s.nav("home")}
            }
        }
    }
}

@Composable
private fun MetricCard(metric:Pair<String,String>,modifier:Modifier) {
    Column(modifier.padding(horizontal=14.dp,vertical=16.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(metric.first,style=PicoTheme.typography.bodySmall,textAlign=TextAlign.Center)
        Text(metric.second,style=PicoTheme.typography.titleLarge,fontWeight=FontWeight.Bold,color=GuardianColors.sageText,textAlign=TextAlign.Center)
    }
}

private fun healthLabel(s:GuardianState,r:SessionRecord)=when(r.metrics["band"]?.toInt()) {
    0 -> s.text("低风险提示","Low risk reference")
    1 -> s.text("注意日常颈部感受","Notice your daily neck symptoms")
    else -> s.text("建议关注，并咨询专业人员","Consider discussing your symptoms with a professional")
}

private fun resultMetrics(s:GuardianState,r:SessionRecord):List<Pair<String,String>> {
    fun angle(k:String)="${fmt(r.metrics[k]?:0.0)}°"
    fun duration()=clock(r.metrics["duration"]?:0.0)
    return when(r.kind) {
        "tower" -> listOf(s.text("平均倾斜角度","Average downward tilt") to angle("average"),s.text("挑战完成比例","Challenge completed") to "${fmt((r.metrics["completion"]?:0.0)*100)}%",s.text("累计宽容时长","Total over-limit time") to clock(r.metrics["over"]?:0.0))
        "flight" -> listOf(s.text("收集星星","Stars collected") to (r.metrics["stars"]?:0.0).toInt().toString(),s.text("错过星星","Stars missed") to (r.metrics["missed"]?.toInt()?.toString()?:"—"),s.text("完成率","Completion rate") to flightRate(r))
        "extension" -> listOf(s.text("最大后仰角度","Maximum extension") to angle("extension"),s.text("测试时长","Duration") to duration())
        else -> listOf(s.text("左侧最大侧伸","Maximum left bend") to angle("left"),s.text("右侧最大侧伸","Maximum right bend") to angle("right"),s.text("测试时长","Duration") to duration())
    }
}

@Composable
private fun HistoryPanel(s:GuardianState,locale:java.util.Locale,modifier:Modifier) {
    Column(modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
            val records=s.history.read().reversed()
            if(records.isEmpty()) Column(Modifier.fillMaxWidth().padding(vertical=48.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(16.dp)) {
                Text(s.text("还没有记录","No records yet"),fontWeight=FontWeight.Bold)
                Text(s.text("完成一次测试或训练后，记录会出现在这里。","Complete a check or training session to see your records here."),color=PicoTheme.colorScheme.labelSecondary)
            }
            records.forEach { r ->
                Row(Modifier.fillMaxWidth().heightIn(min=80.dp).padding(horizontal=14.dp,vertical=14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                    Canvas(Modifier.size(14.dp)) {drawCircle(when(r.kind) {"flight"->GuardianColors.historyPeach;"extension","health"->GuardianColors.historyGold;else->GuardianColors.historyGreen})}
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text(title(r.kind,s.zh),style=PicoTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
                        Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT,locale).format(Date(r.time)),style=PicoTheme.typography.bodySmall,color=PicoTheme.colorScheme.labelSecondary)
                    }
                    Text(historySummary(s,r),style=PicoTheme.typography.bodyMedium,color=GuardianColors.sageText,textAlign=TextAlign.End)
                }
                HorizontalDivider()
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End) {
            SecondaryAction(s.text("返回主菜单","Return home"),"history.home") {s.nav("home")}
        }
    }
}

private fun historySummary(s:GuardianState,r:SessionRecord):String {
    fun a(k:String)="${fmt(r.metrics[k]?:0.0)}°"
    return when(r.kind) {
        "tower" -> s.text("平均 ${a("average")} · 完成 ${fmt((r.metrics["completion"]?:0.0)*100)}%","Avg ${a("average")} · ${fmt((r.metrics["completion"]?:0.0)*100)}% complete")
        "flight" -> if(r.metrics.containsKey("missed")) s.text("${clock(r.metrics["duration"]?:0.0)} · 收集 ${(r.metrics["stars"]?:0.0).toInt()} · 错过 ${r.metrics["missed"]!!.toInt()} · 完成率 ${flightRate(r)}","${clock(r.metrics["duration"]?:0.0)} · Collected ${(r.metrics["stars"]?:0.0).toInt()} · Missed ${r.metrics["missed"]!!.toInt()} · ${flightRate(r)}") else "${clock(r.metrics["duration"]?:0.0)} · ${(r.metrics["stars"]?:0.0).toInt()} ${s.text("颗星","stars") }"
        "extension" -> s.text("最大 ${a("extension")}","Maximum ${a("extension")}")
        "lateral" -> s.text("左 ${a("left")} · 右 ${a("right")}","Left ${a("left")} · Right ${a("right")}")
        else -> healthLabel(s,r)
    }
}
private fun flightRate(r:SessionRecord):String = if((r.metrics["resolved"]?:0.0)>0 && r.metrics.containsKey("completion")) "${fmt(r.metrics["completion"]!!*100)}%" else "—"
@Composable private fun Note(s:GuardianState) {Text(s.text("仅作健康参考，不构成医疗诊断。","For wellbeing reference only. Not a medical diagnosis."),style=PicoTheme.typography.bodySmall,color=PicoTheme.colorScheme.labelSecondary)}
private fun fmt(v:Double)="%.1f".format(v)
private fun clock(v:Double):String {val sec=v.coerceAtLeast(0.0).toInt();return "%02d:%02d".format(sec/60,sec%60)}
private fun title(page:String,zh:Boolean):String {
    val pair=when(page) {
        "disclaimer" -> "使用须知" to "Before you begin"
        "home" -> "颈椎卫士" to "NeckGuardian"
        "trainingSelect" -> "选择挑战训练" to "Choose a challenge"
        "tower","towerSetup" -> "稳姿高塔" to "Posture Tower"
        "flight","flightSetup" -> "舒颈飞行" to "Gentle Glide"
        "extension","extensionSetup" -> "后仰角度测试" to "Neck extension check"
        "lateral","lateralSetup" -> "侧伸角度测试" to "Side bend check"
        "questionnaire","health","healthResult" -> "颈椎健康测试" to "Neck health check"
        "movementSelect" -> "颈椎常规测试" to "Movement checks"
        "history" -> "历史记录" to "History"
        else -> "本次训练结果" to "Session results"
    };return if(zh) pair.first else pair.second
}
