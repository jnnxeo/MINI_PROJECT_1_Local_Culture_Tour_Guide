# public — 그대로 복사되는 파일

빌드할 때 **가공 없이 그대로** 결과물에 복사되는 파일을 둡니다.

| 넣을 것 | 예시 |
|---|---|
| 파비콘 | `favicon.ico` |
| robots.txt | `robots.txt` |
| 코드에서 import 하지 않는 이미지 | `og-image.png` |

## src/assets 와의 차이

| | `public/` | `src/assets/` |
|---|---|---|
| 처리 방식 | 그대로 복사 | 빌드 시 최적화·압축 |
| 사용법 | `<img src="/logo.png">` | `import logo from './assets/logo.png'` |
| 언제 | 파비콘 등 고정 파일 | **대부분의 이미지는 여기** |

> 💡 헷갈리면 `src/assets/`를 쓰세요. 대부분의 경우 그쪽이 맞습니다.
