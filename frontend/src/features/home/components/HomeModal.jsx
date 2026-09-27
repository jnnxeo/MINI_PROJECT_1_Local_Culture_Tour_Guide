import React, { useEffect, useRef } from 'react'
import { createPortal } from 'react-dom'

export default function HomeModal({ title, titleId, onClose, children }) {
  const dialogRef = useRef(null)
  const onCloseRef = useRef(onClose)
  onCloseRef.current = onClose

  useEffect(() => {
    const previousFocus = document.activeElement
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    const firstControl = dialogRef.current?.querySelector('input') || dialogRef.current?.querySelector('button')
    firstControl?.focus()

    const handleKeyDown = (event) => {
      if (event.key === 'Escape') onCloseRef.current()
      if (event.key !== 'Tab') return
      const controls = [...dialogRef.current.querySelectorAll('button:not(:disabled), input:not(:disabled)')]
      const first = controls[0]
      const last = controls[controls.length - 1]
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault()
        last?.focus()
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault()
        first?.focus()
      }
    }

    document.addEventListener('keydown', handleKeyDown)
    return () => {
      document.body.style.overflow = previousOverflow
      document.removeEventListener('keydown', handleKeyDown)
      previousFocus?.focus()
    }
  }, [])

  return createPortal(
    <div className="home-modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose() }}>
      <section className="home-modal" role="dialog" aria-modal="true" aria-labelledby={titleId} ref={dialogRef}>
        <div className="home-modal__heading">
          <h2 id={titleId}>{title}</h2>
          <button className="home-modal__close" type="button" onClick={onClose} aria-label="닫기">×</button>
        </div>
        {children}
      </section>
    </div>,
    document.body,
  )
}
