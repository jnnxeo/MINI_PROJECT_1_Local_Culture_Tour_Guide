# hooks — 재사용 로직 (커스텀 훅)

화면(UI) 없이 **로직만 재사용**하고 싶을 때 씁니다.

## 규칙

파일·함수 이름은 반드시 **`use`로 시작**합니다. (React 규칙)

| 예시 | 하는 일 |
|---|---|
| `useAuth.js` | 로그인 상태·로그아웃 처리 |
| `useDebounce.js` | 검색어 입력이 멈춘 뒤에만 API 호출 |
| `useMap.js` | 지도 초기화·마커 관리 |

## 예시

```js
// useDebounce — 타이핑이 끝난 뒤에만 값을 반영 (검색 자동완성에 유용)
export function useDebounce(value, delay = 300) {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(timer);
  }, [value, delay]);
  return debounced;
}
```

> 💡 처음에는 굳이 훅을 만들 필요 없습니다. **같은 로직이 2~3번 반복될 때** 분리하세요.
