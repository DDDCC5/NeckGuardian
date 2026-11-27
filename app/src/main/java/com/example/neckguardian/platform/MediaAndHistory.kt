package com.example.neckguardian.platform

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import org.json.JSONArray
import org.json.JSONObject
import com.example.neckguardian.domain.recentThirty

data class SessionRecord(val time: Long, val kind: String, val metrics: Map<String, Double>, val status: String)
class HistoryStore(context: Context) {
    private val prefs=context.getSharedPreferences("history",Context.MODE_PRIVATE)
    fun read(): List<SessionRecord> = try {
        val a=JSONArray(prefs.getString("records","[]")); (0 until a.length()).map { i ->
            val r=a.getJSONObject(i); val m=r.getJSONObject("metrics")
            SessionRecord(r.getLong("time"),r.getString("kind"),m.keys().asSequence().associateWith { m.getDouble(it) },r.optString("status"))
        }.takeLast(30)
    } catch (_: Exception) { emptyList() }
    fun add(record:SessionRecord) {
        val a=JSONArray(); recentThirty(read()+record).forEach { r ->
            a.put(JSONObject().put("time",r.time).put("kind",r.kind).put("status",r.status).put("metrics",JSONObject(r.metrics)))
        }
        prefs.edit().putString("records",a.toString()).apply()
    }
}
/** All players are owned by the activity/session and obey system stream volume. */
class Guidance(private val context:Context) {
    private var voice:MediaPlayer?=null
    private var ambience:MediaPlayer?=null
    private var ambiencePath:String?=null
    private val effects=mutableSetOf<MediaPlayer>()
    private fun ambientVolume() {
        val gain=com.example.neckguardian.domain.ambientGain(voice!=null)
        ambience?.let {it.setVolume(gain,gain);android.util.Log.i("NeckGuardian","Ambience gain=$gain speech=${voice!=null}")}
    }
    fun stopVoice() {voice?.release();voice=null;ambientVolume()}
    fun speak(cue:String,zh:Boolean) {
        voice?.release();voice=null
        android.util.Log.i("NeckGuardian","Voice cue=$cue locale=${if(zh) "zh-CN" else "en"}")
        val player=prepared("audio/${if(zh) "zh-CN" else "en"}/$cue.wav",speech=true)
        voice=player;ambientVolume()
        if(player!=null) {
            player.setOnCompletionListener { p -> if(voice===p) {voice=null;p.release();ambientVolume()} }
            player.setOnErrorListener { p,what,extra ->
                android.util.Log.w("NeckGuardian","Voice playback failed what=$what extra=$extra")
                if(voice===p) {voice=null;p.release();ambientVolume()};true
            }
            try {player.start()} catch(e:Exception) {if(voice===player) stopVoice();android.util.Log.w("NeckGuardian","Voice start failed",e)}
        }
    }
    fun effect(cue:String) {
        val player=prepared("audio/sfx/$cue.wav")?:return
        effects.add(player)
        player.setOnCompletionListener { p ->if(effects.remove(p)) p.release() }
        player.setOnErrorListener { p,_,_ ->if(effects.remove(p)) p.release();true }
        try {player.start()} catch(e:Exception) {effects.remove(player);player.release();android.util.Log.w("NeckGuardian","Effect start failed",e)}
    }
    fun startAmbience(path:String?) {
        if(path==ambiencePath && ambience!=null) return
        stopAmbience()
        if(path==null) return
        val player=prepared(path)?:return
        ambience=player;ambiencePath=path;player.isLooping=true;ambientVolume()
        player.setOnErrorListener { p,what,extra ->
            android.util.Log.w("NeckGuardian","Ambience failed what=$what extra=$extra")
            if(ambience===p) stopAmbience();true
        }
        try {player.start();android.util.Log.i("NeckGuardian","Ambience started: $path")}
        catch(e:Exception) {stopAmbience();android.util.Log.w("NeckGuardian","Ambience start failed",e)}
    }
    fun stopAmbience() {if(ambience!=null) android.util.Log.i("NeckGuardian","Ambience stopped");ambience?.release();ambience=null;ambiencePath=null}
    private fun prepared(path:String,speech:Boolean=false):MediaPlayer? {
        val player=MediaPlayer()
        return try {
            context.assets.openFd(path).use { fd ->
                player.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(if(speech) AudioAttributes.CONTENT_TYPE_SPEECH else AudioAttributes.CONTENT_TYPE_MUSIC).build())
                player.setDataSource(fd.fileDescriptor,fd.startOffset,fd.length);player.prepare()
            }
            player
        } catch(e:Exception) {player.release();android.util.Log.e("NeckGuardian","Audio $path",e);null}
    }
    fun stop() {stopAmbience();stopVoice();effects.toList().forEach {it.release()};effects.clear()}
}
