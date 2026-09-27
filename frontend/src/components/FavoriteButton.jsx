import React, { useEffect, useState } from 'react'
import { addFavorite, removeFavorite } from '../services/favoriteApi.js'
import { getErrorMessage } from '../services/authService.js'

function FavoriteButton({ eventContentId, initialFavorite = false, onRemoved }) {
  const [isFavorite, setIsFavorite] = useState(initialFavorite)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    setIsFavorite(initialFavorite)
    setError('')
  }, [eventContentId, initialFavorite])

  const handleFavorite = async () => {
    if (!eventContentId || saving) return
    setSaving(true)
    setError('')

    try {
      if (isFavorite) {
        await removeFavorite(eventContentId)
        setIsFavorite(false)
        onRemoved?.(eventContentId)
      } else {
        await addFavorite(eventContentId)
        setIsFavorite(true)
      }
    } catch (requestError) {
      setError(getErrorMessage(requestError))
    } finally {
      setSaving(false)
    }
  }

  return (
    <>
      <button
        type="button"
        onClick={handleFavorite}
        disabled={!eventContentId || saving}
        aria-label={isFavorite ? '관심 행사 해제' : '관심 행사 저장'}
        aria-pressed={isFavorite}
      >
        {isFavorite ? '♥' : '♡'}
      </button>
      {error && <span role="alert">{error}</span>}
    </>
  )
}

export default FavoriteButton
