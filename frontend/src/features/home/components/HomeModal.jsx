import React, { useEffect, useRef } from 'react'
import { createPortal } from 'react-dom'

export default function HomeModal({ title, titleId, onClose, children, className = '' }) {
  const dialogRef = useRef(null)
  const onCloseRef = useRef(onClose)
  onCloseRef.current = onClose

  useEffect(() => {
    const previousFocus = document.activeElement
    const previousOverflow = document.body.style.overflow
    const dialog = dialogRef.current
    const backdrop = dialog.parentElement
    const backgroundElements = [...document.body.children]
      .filter((element) => element !== backdrop)
      .map((element) => ({ element, inert: element.inert }))
    backgroundElements.forEach(({ element }) => { element.inert = true })
    document.body.style.overflow = 'hidden'
    const firstControl = dialogRef.current?.querySelector('input') || dialogRef.current?.querySelector('button')
    firstControl?.focus()

    const handleKeyDown = (event) => {
      if (event.key === 'Escape' && !event.isComposing) {
        event.preventDefault()
        onCloseRef.current()
      }
      if (event.key !== 'Tab') return
      const controls = [...dialog.querySelectorAll('a[href], button, input, select, textarea, [tabindex]')]
        .filter((element) => element.tabIndex >= 0 && !element.matches(':disabled') && !element.closest('[inert]') && element.getClientRects().length > 0 && getComputedStyle(element).visibility !== 'hidden')
      const first = controls[0]
      const last = controls[controls.length - 1]
      if (!first) {
        event.preventDefault()
        dialog.focus()
      } else if (!dialog.contains(document.activeElement) || document.activeElement === dialog) {
        event.preventDefault()
        const nextControl = event.shiftKey ? last : first
        nextControl.focus()
      } else if (event.shiftKey && document.activeElement === first) {
        event.preventDefault()
        last?.focus()
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault()
        first?.focus()
      }
    }

    const handleFocusIn = (event) => {
      if (!dialog.contains(event.target)) dialog.focus()
    }

    document.addEventListener('keydown', handleKeyDown)
    document.addEventListener('focusin', handleFocusIn)
    return () => {
      document.body.style.overflow = previousOverflow
      document.removeEventListener('keydown', handleKeyDown)
      document.removeEventListener('focusin', handleFocusIn)
      backgroundElements.forEach(({ element, inert }) => { element.inert = inert })
      previousFocus?.focus()
    }
  }, [])

  return createPortal(
    <div className="home-modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose() }}>
      <section className={`home-modal ${className}`.trim()} role="dialog" aria-modal="true" aria-labelledby={titleId} tabIndex={-1} ref={dialogRef}>
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
