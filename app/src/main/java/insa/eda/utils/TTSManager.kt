package insa.eda.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale
import java.util.UUID

class TTSManager(context: Context) {
    private val tag = "TTSManager"
    
    private var tts: TextToSpeech? = null
    private var isReady = false
    
    private val messageCooldowns = mutableMapOf<String, Long>()
    private val COOLDOWN_MS = 10000L
    
    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.KOREAN)
                
                if (result == TextToSpeech.LANG_MISSING_DATA || 
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(tag, "한국어가 지원되지 않습니다. 기본 언어로 설정합니다.")
                    tts?.setLanguage(Locale.getDefault())
                }
                
                tts?.setSpeechRate(1.0f)
                tts?.setPitch(1.0f)
                
                isReady = true
                Log.d(tag, "TTS 초기화 완료")
            } else {
                Log.e(tag, "TTS 초기화 실패: $status")
            }
        }
        
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d(tag, "음성 안내 시작: $utteranceId")
            }
            
            override fun onDone(utteranceId: String?) {
                Log.d(tag, "음성 안내 완료: $utteranceId")
            }
            
            override fun onError(utteranceId: String?) {
                Log.e(tag, "음성 안내 오류: $utteranceId")
            }
        })
    }
    
    fun speak(message: String, messageType: String = "default") {
        if (!isReady) {
            Log.w(tag, "TTS가 아직 준비되지 않았습니다")
            return
        }
        
        val currentTime = System.currentTimeMillis()
        val lastSpeakTime = messageCooldowns[messageType] ?: 0L
        
        if (currentTime - lastSpeakTime > COOLDOWN_MS) {
            val utteranceId = UUID.randomUUID().toString()
            tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            messageCooldowns[messageType] = currentTime
            
            Log.d(tag, "음성 안내 재생: $message (타입: $messageType)")
        } else {
            Log.d(tag, "음성 안내 쿨다운 중: $messageType, 남은 시간: ${COOLDOWN_MS - (currentTime - lastSpeakTime)}ms")
        }
    }
    
    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
        Log.d(tag, "TTS 리소스 해제")
    }
}
