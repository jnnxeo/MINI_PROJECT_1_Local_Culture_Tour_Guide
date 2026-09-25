/** 마지막 글자에 받침이 있는지 (한글이 아니면 false) */
function hasFinalConsonant(word) {
  const code = word.trim().at(-1)?.charCodeAt(0) ?? 0

  if (code < 0xac00 || code > 0xd7a3) {
    return false
  }

  return (code - 0xac00) % 28 !== 0
}

/** "한옥 카페" → "한옥 카페를", "종로 한식당" → "종로 한식당을" */
export function withObject(word) {
  return `${word}${hasFinalConsonant(word) ? '을' : '를'}`
}

/** "서촌 골목 산책" → "서촌 골목 산책이", "한옥 카페" → "한옥 카페가" */
export function withSubject(word) {
  return `${word}${hasFinalConsonant(word) ? '이' : '가'}`
}
