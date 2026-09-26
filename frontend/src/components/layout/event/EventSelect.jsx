import React, { useEffect, useId, useRef, useState } from 'react'

export default function EventSelect({ label, value, options, onChange }) {
  const [open, setOpen] = useState(false)
  const rootRef = useRef(null)
  const triggerRef = useRef(null)
  const optionRefs = useRef([])
  const listId = useId()
  const selectedIndex = options.findIndex((option) => option.value === value)
  const selectedLabel = options[selectedIndex]?.label ?? '선택하세요'

  useEffect(() => {
    if (!open) return undefined

    const closeOnOutsideClick = (event) => {
      if (!rootRef.current?.contains(event.target)) setOpen(false)
    }

    document.addEventListener('pointerdown', closeOnOutsideClick)
    return () => document.removeEventListener('pointerdown', closeOnOutsideClick)
  }, [open])

  const focusOption = (index) => {
    requestAnimationFrame(() => optionRefs.current[index]?.focus())
  }

  const choose = (option) => {
    onChange(option.value)
    setOpen(false)
    triggerRef.current?.focus()
  }

  return (
    <div
      ref={rootRef}
      className="event-select"
      onBlur={(event) => {
        if (!event.currentTarget.contains(event.relatedTarget)) setOpen(false)
      }}
      onKeyDown={(event) => {
        // 목록만 닫고 공통 Modal의 Escape 닫기는 실행하지 않는다.
        if (open && event.key === 'Escape') {
          event.stopPropagation()
          setOpen(false)
          triggerRef.current?.focus()
        }
      }}
    >
      <button
        ref={triggerRef}
        type="button"
        className="event-select__trigger"
        aria-label={`${label}: ${selectedLabel}`}
        aria-haspopup="listbox"
        aria-expanded={open}
        aria-controls={listId}
        onClick={() => setOpen((current) => !current)}
        onKeyDown={(event) => {
          if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
            event.preventDefault()
            setOpen(true)
            focusOption(Math.max(selectedIndex, 0))
          }
        }}
      >
        <span className="event-select__label">{label}</span>
        <span className="event-select__value">{selectedLabel}</span>
      </button>

      {open && (
        <div id={listId} className="event-select__menu" role="listbox" aria-label={label}>
          {options.map((option, index) => (
            <button
              key={option.value}
              ref={(element) => { optionRefs.current[index] = element }}
              type="button"
              role="option"
              aria-selected={option.value === value}
              className={`event-select__option${option.value === value ? ' is-selected' : ''}`}
              onClick={() => choose(option)}
              onKeyDown={(event) => {
                if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
                  event.preventDefault()
                  const direction = event.key === 'ArrowDown' ? 1 : -1
                  optionRefs.current[(index + direction + options.length) % options.length]?.focus()
                }
              }}
            >
              {option.label}
            </button>
          ))}
        </div>
      )}
    </div>
  )
}