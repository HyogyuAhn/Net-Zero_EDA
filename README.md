# 2025 Net-Zero Hackathon: EDA

## 개요
이 프로젝트는 사용자의 활동 데이터나 환경 데이터를 기반으로 탄소 중립(Net-Zero) 달성을 돕는 안드로이드 애플리케이션입니다.

### 🌿 1. Eco Driving Assistance
주행 중 탄소배출 저감을 위한 친환경 주행 코칭 어플리케이션

### ✅ 2. 우리 팀 아이디어를 한 줄로 소개해주세요
실시간 주행 데이터를 기반으로 탄소 절감을 코칭해주는 친환경 운전 도우미 앱

#### 🔑 아이디어 핵심 키워드
주행 습관 개선, 연비, 탄소배출, 센서, 코칭, 리포트, 지역 연계

### 🚨 3. 문제 정의
#### 3.1. 해결하고자 하는 문제
급가속, 급제동, 공회전 등 비효율적 운전 습관 → 탄소 배출 증가
많은 운전자들이 자신의 주행 습관의 환경적 영향을 인지하지 못함
도시 중심의 높은 자가용 의존도 → 개인 운전 습관 개선이 핵심 변수

#### 3.2. 개인적 동기
운전병 복무 경험 중 경사로 운전 시 연비 최적화의 중요성을 체감
데이터 기반의 피드백 시스템이 있었다면 더 효율적인 주행 가능
일상의 작은 실천으로 탄소중립 실현 가능하다는 신념

#### 3.3. 탄소중립 기여 방식
운전 습관 개선 → 연료 절감 → CO₂ 배출량 감소
실천 가능성 높은 개인 맞춤형 코칭 → 실질적인 탄소중립 기여
사용자 간 공유/경쟁 요소 도입 → 지역 기반 친환경 문화 확산

### 👥 4. 사용 대상자
초보 운전자 및 연비를 절감하고 싶은 일반 운전자
20~30대 중심 (주행 습관 형성이 가능한 연령대)
운전 빈도 높고, 스마트폰 사용에 익숙한 사용자
지자체/보험사 등과의 연계로 B2B 확장 가능성

### 🔧 5. 기술 및 서비스 구성 요약
① 사용 기술 및 도구
- **Platform**: Android
- **Languages**: Kotlin, Java
- **Core**: 
  - Android Jetpack
  - Kotlin Coroutines for asynchronous programming
- **Architecture**: 
  - MVVM (Model-View-ViewModel)
  - ViewModel, LiveData, Lifecycle
- **UI**: 
  - Material Components for Android
  - ViewBinding / DataBinding
- **Navigation**: 
  - Jetpack Navigation Component
- **Database**: 
  - Room Persistence Library
  - Google Firebase
- **Networking**: 
  - Retrofit2, OkHttp3
- **Azure Services**:
  - Azure OpenAI
  - AutoML & ML Endpoint
- **Google Maps API**: GPS/경로 시각화
- **Figma**: 발표자료 제작

② 사용자 여정 흐름
[차량 탑승] → [앱 실행] → [주행 시작] → [센서 기반 습관 데이터 수집] → [모델 기반 습관 판단] → [자연어 피드백/알림 제공] → [주행 종료] → [데이터 저장 및 리포트 제공]

③ 핵심 기능 및 반응
실시간 피드백: 급가속/급제동 시 경고 알림, 음성 피드백
주행 리포트: 연비/탄소 배출량 시각화
지역 랭킹/인증제도: SNS 공유, 지자체 시상, 보험 할인 등
예상 반응:
"기름값 아낀다" → 실질적 절감
"내가 친환경 실천 중" → 자부심
"주변 친구와 랭킹 경쟁" → 참여 지속성 향상

## 주요 기능
- **사용자 인증**: 로그인 및 회원가입 기능
- **홈 화면**: 앱의 메인 대시보드
- **통계**: 활동 및 데이터 관련 통계 제공
- **센서 연동**: 디바이스 센서를 활용한 데이터 수집
- **로컬 데이터베이스**: 앱 내 데이터 저장 및 관리

## 프로젝트 구조
```
app
└── src
    └── main
        └── java
            └── insa
                └── eda
                    ├── activities/      # 액티비티 (화면)
                    ├── adapters/        # 리사이클러뷰 어댑터
                    ├── database/        # 로컬 데이터베이스 (Room, SQLite 등)
                    ├── fragments/       # 프래그먼트
                    ├── navigation/      # Kakao Navigation SDK
                    ├── sensors/         # 센서 관련 로직
                    ├── services/        # 백그라운드 서비스
                    ├── utils/           # 유틸리티 클래스
                    ├── ActivityHome.java
                    ├── LoginActivity.java
                    ├── RegisterActivity.java
                    └── MainApplication.kt
```
  
