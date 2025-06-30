# StoryWave

StoryWave는 사용자들이 협력하여 이야기를 만들 수 있는 실시간 협업 스토리텔링 플랫폼입니다. 이 문서는 StoryWave API의 사용 방법을 설명합니다.

## 목차

- [개요](#개요)
- [인증 API](#인증-api)
- [방 매칭 API](#방-매칭-api)
- [스토리 API (RSocket)](#스토리-api-rsocket)
- [속도 제한](#속도-제한)
- [환경 설정](#환경-설정)

## 개요

StoryWave API는 크게 세 부분으로 구성됩니다:

1. **REST API**: 인증 및 방 매칭을 위한 HTTP 기반 API
2. **Server-Sent Events (SSE)**: 실시간 방 매칭 이벤트를 위한 스트림
3. **RSocket API**: 실시간 스토리 작성 및 업데이트를 위한 양방향 통신

대부분의 API는 사용자 인증이 필요합니다. 게스트 로그인을 통해 간단히 임시 사용자 ID를 발급받을 수 있습니다.

## 인증 API

### 게스트 로그인

새로운 게스트 사용자를 생성하고 임시 ID를 발급받습니다.

- **URL**: `/api/auth/guest`
- **Method**: `POST`
- **요청 본문**: 없음
- **응답**:
  ```json
  {
    "id": "사용자ID",
    "createdAt": "생성일시(ISO-8601 형식)"
  }
  ```
- **속도 제한**: IP 당 1시간에 10회 (기본값, 구성 가능)

## 방 매칭 API

### 대기열 구독

대기열 상태 및 방 매칭 이벤트를 구독합니다.

- **URL**: `/api/room/subscribe/{userId}`
- **Method**: `GET`
- **Content-Type**: `text/event-stream` (SSE)
- **Path 파라미터**:
  - `userId`: 사용자 ID
- **이벤트 유형**:
  - `MATCHED`: 사용자가 방에 매칭되었을 때 전송됨
    ```json
    {
      "type": "MATCHED",
      "roomId": "방ID",
      "userIds": ["사용자ID1", "사용자ID2", "..."]
    }
    ```

### 대기열 추가

매칭 대기열에 사용자를 추가합니다.

- **URL**: `/api/room/subscribe`
- **Method**: `POST`
- **요청 파라미터**:
  - `userId`: 사용자 ID
- **응답**: 성공 시 `true`, 실패 시 `false`
- **참고**: 대기열에 충분한 사용자가 모이면 자동으로 방이 생성되고 매칭됩니다.

## 스토리 API (RSocket)

StoryWave는 RSocket 프로토콜을 사용하여 실시간 스토리 작성 기능을 제공합니다.

### RSocket 연결

- **URL**: `/rsocket`
- **Transport**: WebSocket
- **Port**: 8888

### 연결 설정

사용자 ID로 RSocket 연결을 설정합니다.

- **Route**: `user.{userId}`
- **Payload**: 없음

### 스토리 이벤트 구독

특정 방의 스토리 이벤트를 구독합니다.

- **Route**: `story.room.{roomId}`
- **Method**: `REQUEST_STREAM`
- **Path 파라미터**:
  - `roomId`: 방 ID
- **응답**: `StoryEvent` 스트림
  ```json
  {
    "type": "STORY_CREATED | LINE_ADDED | STORY_COMPLETED",
    "story": {
      "id": "스토리ID",
      "roomId": "방ID",
      "lines": [
        {
          "userId": "사용자ID",
          "content": "스토리 내용",
          "timestamp": "작성시간(ISO-8601 형식)"
        }
      ],
      "completed": false,
      "maxRound": 3
    }
  }
  ```

### 스토리 라인 추가

스토리에 새 라인을 추가합니다.

- **Route**: `story.line.{storyId}`
- **Method**: `REQUEST_RESPONSE`
- **Path 파라미터**:
  - `storyId`: 스토리 ID
- **Payload**:
  ```json
  {
    "userId": "사용자ID",
    "content": "추가할 스토리 내용"
  }
  ```
- **응답**: 성공 시 `true`, 실패 시 `false`

### 스토리 정보 조회

특정 스토리의 정보를 조회합니다.

- **Route**: `story.info.{storyId}`
- **Method**: `REQUEST_RESPONSE`
- **Path 파라미터**:
  - `storyId`: 스토리 ID
- **응답**: 스토리 객체
  ```json
  {
    "id": "스토리ID",
    "roomId": "방ID",
    "lines": [
      {
        "userId": "사용자ID",
        "content": "스토리 내용",
        "timestamp": "작성시간(ISO-8601 형식)"
      }
    ],
    "completed": false,
    "maxRound": 3
  }
  ```

## 속도 제한

StoryWave는 API 요청에 대한 속도 제한을 구현하여 서비스의 안정성을 보장합니다.

- **인증 API**: IP 당 시간당 10회 (기본값)
- **설정 변경**: `application.properties`에서 다음 속성으로 구성 가능
  ```properties
  storywave.auth.rate-limit.max-requests=10
  storywave.auth.rate-limit.window-hours=1
  ```

## 환경 설정

다음은 StoryWave의 주요 구성 속성입니다:

```properties
# Redis 설정
storywave.auth.use-redis=true
storywave.room.use-redis=true
storywave.story.use-redis=true

# 방 매칭 설정
storywave.room.required-users=4

# 스토리 설정
storywave.story.default-max-round=3

# RSocket 서버 설정
spring.rsocket.server.transport=websocket
spring.rsocket.server.mapping-path=/rsocket
spring.rsocket.server.port=8888

# 속도 제한 설정
storywave.auth.rate-limit.max-requests=10
storywave.auth.rate-limit.window-hours=1
