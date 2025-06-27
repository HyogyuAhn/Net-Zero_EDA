package insa.eda.database.models

import java.util.Date

/**
 * Firebase와 호환되는 주행 기록 데이터 모델
 *
 * @param id 기록 ID (기존 코드와의 호환성을 위해 Int형 지원)
 * @param recordId Firebase에서 사용하는 문서 ID
 * @param userId 사용자 ID - Firebase에서는 문자열로 사용
 * @param startTime 주행 시작 시간
 * @param endTime 주행 종료 시간
 * @param duration 주행 시간(초)
 * @param distance 주행 거리(km)
 * @param avgSpeed 평균 속도(km/h)
 * @param co2Saved 절약한 CO2랭(키로그램)
 * @param ecoScore 에코 점수(0-100)
 * @param createdAt 기록 생성 시간
 */
data class DrivingRecord(
    var id: Int? = null,
    var recordId: String? = null,
    var userId: String = "",
    var startTime: Date? = null,
    var endTime: Date? = null,
    var duration: Int = 0,
    var distance: Float = 0f,
    var avgSpeed: Float = 0f,
    var fuelEfficiency: Float = 0f,
    var co2Emission: Float = 0f,
    var co2Saved: Float = 0f,
    var ecoScore: Int = 0,
    var rapidAcceleration: Int = 0,
    var hardBraking: Int = 0,
    var sharpTurns: Int = 0, 
    var idlingTime: Int = 0,
    var createdAt: Date? = null
)
