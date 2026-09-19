# 00. 깃허브 기초 가이드 (처음 쓰는 사람용)

> 깃허브를 한 번도 안 써봤어도 이 문서만 따라 하면 작업할 수 있습니다.
> 용어가 낯설어도 괜찮습니다. **일단 순서대로 따라 해 보고**, 나중에 다시 읽으면 이해됩니다.

---

## 1. 개념부터 5분만

### Git과 GitHub는 다릅니다

| | Git | GitHub |
|---|---|---|
| 무엇 | 내 컴퓨터에서 돌아가는 **버전 관리 프로그램** | Git 저장소를 **인터넷에 올려두는 사이트** |
| 비유 | 문서의 "저장 기록"을 남기는 기능 | 그 문서를 팀원과 공유하는 클라우드 |

### 작업이 흘러가는 순서

```
  [ 내 컴퓨터 ]                                   [ GitHub 서버 ]

  작업 폴더  ──git add──▶  스테이지  ──git commit──▶  로컬 저장소
                                                          │
                                                      git push
                                                          ▼
                                                     원격 저장소
                                                          │
                                                      git pull
                                                          ▼
                                                      내 컴퓨터
```

| 명령어 | 한 줄 설명 | 비유 |
|---|---|---|
| `git clone` | GitHub에 있는 코드를 내 컴퓨터로 **처음 한 번** 복사 | 파일 내려받기 |
| `git add` | "이 파일을 저장 기록에 포함시킬게" 라고 표시 | 장바구니에 담기 |
| `git commit` | 표시한 파일들을 **저장 기록으로 확정** (내 컴퓨터에만) | 결제 확정 |
| `git push` | 내 컴퓨터의 커밋을 **GitHub에 올림** | 업로드 |
| `git pull` | GitHub의 최신 내용을 **내 컴퓨터로 내려받음** | 다운로드 |
| `git branch` | 작업 공간을 **따로 만들어** 서로 방해 안 하게 함 | 복사본에서 작업 |

> 💡 **왜 add와 commit이 나뉘어 있나요?**
> 10개 파일을 고쳤어도 그중 3개만 저장 기록에 남기고 싶을 수 있기 때문입니다.
> `add`로 고른 뒤 `commit`으로 확정합니다.

---

## 2. 최초 1회만 하는 설정

내가 누구인지 Git에 알려줍니다. (커밋 기록에 이름으로 남습니다)

```bash
git config --global user.name "본인 GitHub 아이디"
git config --global user.email "GitHub에 등록한 이메일"
```

확인:

```bash
git config --global user.name
git config --global user.email
```

---

## 3. 프로젝트 내려받기 (clone)

```bash
# 원하는 폴더로 이동 (예: 바탕화면)
cd ~/Desktop

# 저장소 복사
git clone https://github.com/jnnxeo/MINI_PROJECT_1_Local_Culture_Tour_Guide.git

# 폴더 안으로 이동
cd MINI_PROJECT_1_Local_Culture_Tour_Guide
```

> ⚠️ `clone`은 **처음 한 번만** 합니다. 이후에는 `git pull`로 최신화합니다.
> 이미 clone 해놓고 또 clone 하면 폴더가 중복으로 생깁니다.

---

## 4. 매일 작업하는 순서 (이것만 외우면 됩니다)

### ① 작업 시작 전 — 최신 코드 받기

```bash
git checkout develop        # 기준 브랜치로 이동
git pull origin develop     # 팀원들이 올린 최신 코드 받기
```

> 🔥 **이걸 건너뛰면 충돌(conflict)이 납니다.** 작업 시작 전에 무조건 하세요.

### ② 내 작업 브랜치 만들기

```bash
git checkout -b feat/login-api
```

- `-b`는 "브랜치를 새로 만들면서 그쪽으로 이동"이라는 뜻입니다.
- 브랜치 이름 규칙은 [`01_브랜치전략.md`](01_브랜치전략.md) 참고

### ③ 코드 작업

평소처럼 파일을 수정하고 저장합니다.

### ④ 무엇이 바뀌었는지 확인

```bash
git status     # 어떤 파일이 바뀌었는지 목록으로 보기
git diff       # 구체적으로 어떤 줄이 바뀌었는지 보기 (q 키로 빠져나옴)
```

### ⑤ 저장 기록 남기기 (add → commit)

```bash
git add .                                   # 바뀐 파일 전부 담기
git commit -m "feat: 로그인 API 구현"          # 저장 기록 확정
```

> ⚠️ `git add .` 은 편하지만, 실수로 비밀번호 파일까지 담길 수 있습니다.
> `git status`로 **무엇이 담겼는지 꼭 확인**하세요.
> 특정 파일만 담으려면: `git add backend/src/.../LoginController.java`

### ⑥ GitHub에 올리기 (push)

```bash
git push origin feat/login-api
```

> 처음 push할 때 `--set-upstream` 하라는 안내가 나오면 그대로 복사해 실행하면 됩니다.

### ⑦ PR(Pull Request) 만들기

1. GitHub 저장소 페이지에 들어가면 상단에 **"Compare & pull request"** 버튼이 뜹니다.
2. 클릭 → 제목·설명 작성 → **base 브랜치가 `develop`인지 확인** → Create pull request
3. 팀원이 확인(리뷰)한 뒤 Merge 합니다.

> PR = "제 작업 다 됐으니 합쳐 주세요"라고 요청하는 것입니다.
> 바로 develop에 push하지 않고 PR을 거치는 이유는, **합치기 전에 서로 한 번 확인**하기 위해서입니다.

---

## 5. 자주 하는 실수와 해결법

### ❓ main 브랜치에서 작업해버렸어요

아직 커밋 안 했다면 — 브랜치만 새로 만들면 변경 내용이 따라옵니다.

```bash
git checkout -b feat/내작업이름
```

이미 커밋했다면 — 팀장에게 알리고 [`06_자주묻는질문_트러블슈팅.md`](06_자주묻는질문_트러블슈팅.md) 참고

### ❓ pull 했더니 "conflict"(충돌)가 났어요

같은 파일의 같은 줄을 두 사람이 다르게 고쳤을 때 생깁니다. 파일을 열면 이렇게 보입니다.

```
<<<<<<< HEAD
내가 쓴 코드
=======
상대방이 쓴 코드
>>>>>>> develop
```

1. `<<<<<<<`, `=======`, `>>>>>>>` 줄을 **전부 지우고**
2. 최종적으로 남길 코드만 남긴 뒤 저장
3. `git add .` → `git commit` 으로 마무리

> 🙋 **혼자 판단하기 어려우면 상대 팀원과 이야기한 뒤 고치세요.** 남의 코드를 임의로 지우면 안 됩니다.

### ❓ 커밋 메시지를 잘못 썼어요 (아직 push 전)

```bash
git commit --amend -m "올바른 메시지"
```

> ⚠️ 이미 push한 커밋에는 쓰지 마세요. 팀원 기록과 어긋납니다.

### ❓ 방금 한 작업을 되돌리고 싶어요 (아직 commit 전)

```bash
git restore 파일이름        # 특정 파일 되돌리기
git restore .              # 전부 되돌리기 (⚠️ 복구 불가, 신중히)
```

### ❓ vi/vim 편집기가 열려서 못 빠져나가겠어요

`git commit`을 `-m` 없이 실행하면 편집기가 열립니다.

- 메시지 입력: `i` 누르고 타이핑
- 저장 후 종료: `Esc` → `:wq` → `Enter`
- 그냥 취소: `Esc` → `:q!` → `Enter`

---

## 6. 안전 수칙

| 하지 말 것 | 이유 |
|---|---|
| `main` 브랜치에 직접 push | 검토 없이 반영되어 되돌리기 어렵습니다 |
| `git push -f` (강제 push) | 남의 커밋을 지워버릴 수 있습니다 |
| `application.properties`, `.env` 커밋 | DB 비밀번호·API 키 유출 |
| `node_modules/`, `build/` 커밋 | 용량 폭발 + 충돌 유발 |

> 🔐 실수로 비밀번호·API 키를 커밋했다면 **파일을 지우는 것만으로는 부족합니다.**
> 즉시 팀에 알리고 **해당 키를 폐기·재발급**하세요. 커밋 기록에는 계속 남아 있습니다.

---

## 7. 더 편하게 쓰고 싶다면

명령어가 어렵다면 GUI 도구를 써도 됩니다. 기능은 똑같습니다.

- **GitHub Desktop** — 가장 쉬움, 무료 (https://desktop.github.com)
- **VS Code 내장 Git** — 왼쪽 사이드바의 Source Control 아이콘
- **IntelliJ 내장 Git** — 상단 메뉴 Git

> 다만 **어떤 명령이 실행되는지는 알고 쓰는 것**이 좋습니다. 문제가 생겼을 때 해결이 빨라집니다.
