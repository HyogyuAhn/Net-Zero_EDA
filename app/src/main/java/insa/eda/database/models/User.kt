package insa.eda.database.models

import java.util.Date

/**
 * Firebase와 호환되는 사용자 데이터 모델
 *
 * @param id 사용자 ID (Firebase에서는 String 형식을 사용하지만, 기존 애플리케이션 호환성을 위해 Int도 지원)
 * @param uid Firebase 인증의 고유 ID
 * @param name 사용자 이름
 * @param email 사용자 이메일
 * @param phone 사용자 전화번호
 * @param password 사용자 비밀번호 (Firebase에 저장되지 않음)
 * @param createdAt 계정 생성 일시
 */
data class User(
    var uid: String? = null,
    var name: String = "",
    var email: String = "",
    var phone: String = "",
    var password: String = "",
    var createdAt: Date? = null
)