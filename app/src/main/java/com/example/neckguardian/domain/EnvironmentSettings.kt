package com.example.neckguardian.domain

/** Stable persisted IDs; no arbitrary path or future unknown value can select immersion. */
enum class TrainingEnvironment(val id:String) {
    REAL("real"), FOREST("forest"), LAKE("lake"), STUDY("study");
    val modelAsset:String? get()=if(this==REAL) null else "environments/$id-v6.glb"
    // SDR images supply ambient lighting only; all visible scenery is modeled geometry.
    val lightingAsset:String? get()=if(this==REAL) null else "environments/$id.png"
    companion object {
        fun fromId(id:String?)=entries.firstOrNull {it.id==id}?:REAL
    }
}
data class FlightEnvironmentSettings(
    val environment:TrainingEnvironment=TrainingEnvironment.REAL,
    val ambienceEnabled:Boolean=false,
) {
    fun environmentFor(kind:String)=if(kind=="flight") environment else TrainingEnvironment.REAL
    fun ambientPath(kind:String):String? = environmentFor(kind).takeIf {ambienceEnabled && it!=TrainingEnvironment.REAL}?.let {"environments/${it.id}.wav"}
}
fun ambientGain(speechActive:Boolean)=if(speechActive) .25f else 1f
