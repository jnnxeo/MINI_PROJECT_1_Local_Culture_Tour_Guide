import React, { useState } from 'react'
import HomeModal from './HomeModal.jsx'
import DateCalendar from '../../../components/common/DateCalendar.jsx'

export default function DateModal({ dates = [], onConfirm, onClose }) {
  const [selected, setSelected] = useState(dates)
  return (
    <HomeModal title="날짜 선택" titleId="date-modal-title" onClose={onClose}>
      <p className="home-modal__description">갈 수 있는 날을 골라 주세요. 선택한 날 중 하루라도 진행하는 행사를 찾아요. 최대 31일까지 선택할 수 있어요.</p>
      <DateCalendar value={selected} onChange={setSelected} />
      {selected.length > 31 && <p role="alert">날짜는 최대 31일까지 선택해 주세요.</p>}
      <button className="primary-button home-modal__full-button" type="button" disabled={selected.length > 31} onClick={() => onConfirm(selected)}>
        {selected.length ? `${selected.length}일 선택 완료` : '날짜 제한 없이 검색'}
      </button>
    </HomeModal>
  )
}
