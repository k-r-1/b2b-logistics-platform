# Java 21 실행 환경 컴팩트 이미지 지정
FROM eclipse-temurin:21-jre-alpine

# 빌드 시 생성된 jar 파일을 컨테이너 내부로 복사
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

# 서버 구동 명령어 설정
ENTRYPOINT ["java", "-jar", "/app.jar"]