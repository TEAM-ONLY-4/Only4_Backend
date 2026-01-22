FROM amazoncorretto:21-alpine

# [필수] Alpine 리눅스는 tzdata가 없으므로 설치해야 함
RUN apk add --no-cache tzdata

# 한국 시간대 설정
ENV TZ=Asia/Seoul
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 변수 설정
ARG JAR_FILE=build/libs/*SNAPSHOT.jar

# JAR 파일 복사
COPY ${JAR_FILE} app.jar

# [권장] JAVA_OPTS를 추가하여 JVM 옵션을 외부에서 주입 가능하게 변경
# 예: docker run -e JAVA_OPTS="-Xmx512m" ...
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app.jar"]