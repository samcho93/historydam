# 기본 규칙. 필요 시 라이브러리별 keep 규칙 추가.
# kotlinx.serialization, Retrofit, Hilt 등은 각 라이브러리 consumer rules 가 처리.

# 서버·데이터 DTO 와 Retrofit 인터페이스 (직렬화·리플렉션 대상)
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*
-keep class com.samdori93.yeoksadam.core.network.** { *; }
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Room 엔티티
-keep class com.samdori93.yeoksadam.core.database.entity.** { *; }

# 네이버 지도
-keep class com.naver.maps.** { *; }
-dontwarn com.naver.maps.**
