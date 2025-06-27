package insa.eda.database.models

import java.util.Date

/**
 * 주행 중 발생하는 이벤트 데이터 모델
 *
 * @param id 이벤트 ID
 * @param recordId 연결된 주행 기록 ID
 * @param type 이벤트 타입 (급가속, 급제동, 공회전)
 * @param timestamp 이벤트 발생 시간
 * @param intensity 이벤트 강도 (센서값 크기)
 * @param latitude 발생 위치 위도
 * @param longitude 발생 위치 경도
 */
data class DrivingEvent(
    var id: String = "",
    var recordId: String = "",
    var type: DrivingEventType = DrivingEventType.SUDDEN_ACCELERATION,
    var timestamp: Date = Date(),
    var intensity: Float = 0f,
    var latitude: Double? = null,
    var longitude: Double? = null
)
