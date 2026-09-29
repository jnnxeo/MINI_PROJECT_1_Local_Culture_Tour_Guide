import React from 'react'
import palaceImage from '../../../assets/mock/palace.jpg'

export default function HeroSection({ onExplore }) {
  return (
    <section className="home-hero" aria-labelledby="home-title">
      <div className="home-hero__inner">
        <div className="home-hero__copy">
          <p className="home-hero__eyebrow">SEOUL, A DIFFERENT DAY</p>
          <h1 id="home-title">
            좋아하는 문화에서
            <br />
            나만의 여행이 시작됩니다.
          </h1>
          <p className="home-hero__description">
            전시, 공연, 축제를 고르고
            <br />
            그날의 동선은 TripAI와 함께 완성하세요.
          </p>
          <button className="primary-button home-hero__button" type="button" onClick={onExplore}>
            이번 달 행사 둘러보기
          </button>
        </div>

        <div className="home-hero__visual">
          <img className="home-hero__photo" src={palaceImage} alt="파란 하늘 아래 서울 고궁의 처마" />
        </div>
      </div>
    </section>
  )
}
