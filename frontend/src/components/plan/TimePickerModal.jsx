import React, { useRef, useState } from 'react'
import PlanModal from './PlanModal.jsx'
import { formatKoreanTime, MINUTE_STEP, toMinutes, toTime } from '../../utils/planTime.js'

const PERIODS = ['오전', '오후']
const HOURS = Array.from({ length: 12 }, (_, index) => (index === 0 ? 12 : index))
const MINUTES = Array.from({ length: 60 / MINUTE_STEP }, (_, index) => index * MINUTE_STEP)
const DURATIONS = [30, 45, 60, 90, 120]

/**
 * Figma "time · 팝업" — 오전/오후 · 시 · 분 세 줄 선택 + 머무는 시간 칩
 * 선택값 위아래 두 칸씩 보여 주고, 클릭·휠·방향키로 옮긴다.
 */
function WheelColumn({ label, values, selectedIndex, cyclic, format = String, onChange }) {
  // 트랙패드는 휠 이벤트가 잘게 많이 와서, 일정량이 쌓일 때만 한 칸 옮긴다.
  const wheelDelta = useRef(0)

  const move = (step) => {
    const next = selectedIndex + step

    if (cyclic) {
      onChange((next + values.length) % values.length)
    } else if (next >= 0 && next < values.length) {
      onChange(next)
    }
  }

  return (
    <div
      className="plan-wheel__column"
      role="listbox"
      aria-label={label}
      tabIndex={0}
      onWheel={(event) => {
        wheelDelta.current += event.deltaY

        if (Math.abs(wheelDelta.current) >= 40) {
          move(wheelDelta.current > 0 ? 1 : -1)
          wheelDelta.current = 0
        }
      }}
      onKeyDown={(event) => {
        if (event.key === 'ArrowDown') {
          event.preventDefault()
          move(1)
        }

        if (event.key === 'ArrowUp') {
          event.preventDefault()
          move(-1)
        }
      }}
    >
      <span className="plan-wheel__label">{label}</span>
      {[-2, -1, 0, 1, 2].map((offset) => {
        const rawIndex = selectedIndex + offset
        const index = cyclic ? (rawIndex + values.length) % values.length : rawIndex
        const exists = index >= 0 && index < values.length
        const distance = Math.abs(offset)

        return (
          <button
            key={offset}
            className={`plan-wheel__cell plan-wheel__cell--d${distance}${exists ? '' : ' is-empty'}`}
            type="button"
            tabIndex={-1}
            role="option"
            aria-selected={offset === 0}
            disabled={!exists}
            onClick={() => exists && onChange(index)}
          >
            {exists ? format(values[index]) : '—'}
          </button>
        )
      })}
    </div>
  )
}

export default function TimePickerModal({ title = '방문 시간 변경', startTime, durationMin, onApply, onClose }) {
  const initialMinutes = toMinutes(startTime)
  const initialHour = Math.floor(initialMinutes / 60)
  const [periodIndex, setPeriodIndex] = useState(initialHour >= 12 ? 1 : 0)
  const [hourIndex, setHourIndex] = useState(HOURS.indexOf(initialHour % 12 === 0 ? 12 : initialHour % 12))
  const [minuteIndex, setMinuteIndex] = useState(Math.round((initialMinutes % 60) / MINUTE_STEP) % MINUTES.length)
  const [duration, setDuration] = useState(durationMin)

  const hour24 = (HOURS[hourIndex] % 12) + (periodIndex === 1 ? 12 : 0)
  const selectedTime = toTime(hour24 * 60 + MINUTES[minuteIndex])
  const durations = [...new Set([...DURATIONS, durationMin])].sort((a, b) => a - b)

  return (
    <PlanModal title={title} onClose={onClose}>
      <p className="tp-modal__desc">선택한 행사 시간과 겹치지 않도록 조정해 주세요. 분은 5분 단위입니다.</p>

      <div className="plan-picker">
        <div className="plan-picker__box">
          <span className="plan-picker__label">시작 시간</span>
          <div className="plan-wheel">
            <WheelColumn label="오전/오후" values={PERIODS} selectedIndex={periodIndex} onChange={setPeriodIndex} />
            <WheelColumn label="시" values={HOURS} selectedIndex={hourIndex} cyclic onChange={setHourIndex} />
            <WheelColumn
              label="분"
              values={MINUTES}
              selectedIndex={minuteIndex}
              cyclic
              format={(minute) => String(minute).padStart(2, '0')}
              onChange={setMinuteIndex}
            />
          </div>
          <p className="plan-picker__selected">{`선택: ${formatKoreanTime(selectedTime)}`}</p>
        </div>

        <div className="plan-picker__box">
          <span className="plan-picker__label">머무는 시간 (5분 단위)</span>
          <div className="plan-chips" role="radiogroup" aria-label="머무는 시간">
            {durations.map((minutes) => (
              <button
                key={minutes}
                className={`plan-chip${minutes === duration ? ' is-selected' : ''}`}
                type="button"
                role="radio"
                aria-checked={minutes === duration}
                onClick={() => setDuration(minutes)}
              >
                {`${minutes}분`}
              </button>
            ))}
          </div>
        </div>
      </div>

      <button
        className="tp-btn tp-btn--primary tp-btn--block"
        type="button"
        onClick={() => onApply({ startTime: selectedTime, durationMin: duration })}
      >
        변경 적용
      </button>
    </PlanModal>
  )
}
