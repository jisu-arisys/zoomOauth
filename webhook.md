# ZOOM webhook 연동

zoom 에서 보내주는 실시간 이벤트를 받기 위함


### 설정화면에서 수신받을 이벤트 및 경로 설정

https://marketplace.zoom.us/
url 은 CA 인증받은 https://[DOMAIN] 형식이어야함.

ngrok 을 사용해 CA인증된 도메인을 내 로컬포트 localhost:9090에 포워딩하여 테스트함.
https://dashboard.ngrok.com/get-started/setup/macos

```
ngrok config add-authtoken 33OjsKe0sTvLsRz5IzDyAsf18P7_7QzBFR1zjK658wFJSt5Dw
ngrok http 80
```

### webhook 엔트포인트 검증 과정 <<endpoint.url_validation>>

zoom 에서 등록된 url 로 보낸 요청에 정해진 양식의 응답을 전달해야 이후 해당 경로로 웹훅 이벤트가 전달됨.
1. 서명 검증 : body 를 JSON 형식으로 직렬화하여 zoom 이 보낸 헤더의 x-zm-signature 값과 일치하는지 확인 
2. JSON 응답 : 3초이내에 plainToken 을 secret token 으로 해싱한 값을 body 에 담아 200 응답 전달 해야함.

```
{"payload":{"plainToken":"2oXY7bAKQLCwXDMECt8m_g"},"event_ts":1759205906459,"event":"endpoint.url_validation"}

v0=cc554ff6791064729e68faf78c9e97e9f49d8a1f2094deabbf1b22076a6dc187

{"plainToken":"2oXY7bAKQLCwXDMECt8m_g","encryptedToken":"7c8fb1a685a782598c47b2cc75da736108fb1d7ceb223837f7ae69c1bdefb3ac"}
```

검증 이후 받은 webhook 이벤트
```
{event=user.presence_status_updated, 
payload={account_id=RuKYKI0gRmioLXZxGXzq2Q, 
object={date_time=2025-09-30T04:22:35Z, 
email=strategos@arisys.co.kr, 
id=prgjvqabtrcaaxoq43x97a, presence_status=Available}}, 
event_ts=1759206155113}
```

### webhook 엔트포인트 검증 검증주기

웹훅 URL은 72시간마다 주기적으로 재검증됩니다. URL이 재검증에 실패하면 Zoom은 다음 일정에 따라 앱과 연결된 계정 소유자에게 알림 이메일을 보냅니다.
[ 연속으로 2번의 재검증 실패 > 첫 번째 알림 이메일 > 4번 연속으로 재검증에 실패 > 두 번째 알림 이메일 > 재검증이 6회 연속 실패 > 이벤트 구독을 비활성화 ]
웹훅 이벤트를 다시 수신하려면 앱 웹훅 엔드포인트 URL 설정으로 이동하여 웹훅을 다시 활성화하고, 필요한 경우 엔드포인트 URL을 업데이트한 후, '검증'을 클릭하여 엔드포인트 URL을 재검증하고 마지막으로 '저장'을 클릭하세요.


### 화면

1. http://localhost:8080/zoom 에서 /account_credentials/token 직접 요청
2. http://localhost:8080/list 에서 페이지 로드시점에 ajax('/api/users') 요청으로 전체 사용자 목록을 불러와 카드 생성 (97명)
3. 사용자의 상태 변경시 웹훅 이벤트를 받으면, queue 에 담았다가, 클라이언트에게 websocket 으로 전달 -> 수신한 클라이언트의 화면의 상태값 변경됨.

문제 : 최초 사용자 정보 호출시 모든 사용자의 상태가 default: active 로 표기되는 문제가 있음.
