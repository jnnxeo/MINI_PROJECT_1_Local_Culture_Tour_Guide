# assets — 이미지·폰트

코드에서 `import`해서 쓰는 이미지·폰트 파일을 둡니다.

```jsx
import logo from '../assets/logo.png';

<img src={logo} alt="로고" />
```

## public/ 과의 차이

| | `assets/` | `public/` |
|---|---|---|
| 처리 | 빌드 시 최적화 | 그대로 복사 |
| 대부분의 경우 | ✅ 여기 | 파비콘 등만 |

## 주의

⚠️ **용량이 큰 이미지는 올리지 마세요.** 저장소가 무거워집니다.
필요하면 미리 압축하거나(TinyPNG 등), 외부 이미지 URL을 사용하세요.
