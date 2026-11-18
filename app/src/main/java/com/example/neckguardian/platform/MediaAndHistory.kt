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
class Guidance(private val context:Context) {
    private var voice:MediaPlayer?=null
    private val effects=mutableSetOf<MediaPlayer>()
    fun stopVoice() {voice?.release();voice=null}
    fun speak(cue:String,zh:Boolean) { stopVoice();android.util.Log.i("NeckGuardian","Voice cue=$cue locale=${if(zh) "zh-CN" else "en"}");voice=play("audio/${if(zh) "zh-CN" else "en"}/$cue.wav") }
    fun effect(cue:String) { play("audio/sfx/$cue.wav")?.let { effects.add(it); it.setOnCompletionListener { p -> effects.remove(p);p.release() } } }
    private fun play(path:String):MediaPlayer? = try {
        context.assets.openFd(path).use { fd -> MediaPlayer().apply {
            setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            setDataSource(fd.fileDescriptor,fd.startOffset,fd.length);prepare();start()
        } }
    } catch(e:Exception) { android.util.Log.e("NeckGuardian","Audio $path",e);null }
    fun stop() { stopVoice();effects.toList().forEach { it.release() };effects.clear() }
}
