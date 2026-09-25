import React, { useState, useEffect } from 'react'
import api from '../services/api.js'
import '../styles/signup.css'

import PlansList from '../components/list/PlansList'
import EventsList from '../components/list/EventsList'

const MyPage = () => {

    //페이지 훅
    const [activeTab, setActiveTab] = useState("plans");

    const [plans, setPlans] = useState([]);
    const [events, setEvents] = useState([]);

    const [page, setPage] = useState(0);
    const [pageSize, setPageSize] = useState(10);
    const [totalPages, setTotalPages] = useState(0);

    const loadPlans = async () =>{

        await api.get('/api/plans', {
            params : {
                page : page,
                size : pageSize
            }
        })
        .then(response => {
            console.log('Mypage loadPlans response : ', response)
            if(response.status === 200){
                const result = response.data.data
                setPlans(result.plans)
                const totalPageCount = Math.ceil(result.totalCount / pageSize)
                setTotalPages(totalPageCount)
            }
        })
        .catch(error=>{
            if(error.response?.status === 404){
                    setPlans([])
                    setTotalPages(0)
            }
        })
    }

    const loadEvents = async () => {

        await api.get('/api/favorites/events', {
            params : {
                page : page,
                size : pageSize
            }
        })
        .then(response => {
                console.log(`debug >>>> MyPage loadEvents response : `, response)
                if(response.status === 200){
                    const result = response.data.data
                    setEvents(result.events)
                    const totalPageCount = Math.ceil(result.totalCount / pageSize);
                    setTotalPages(totalPageCount);
                }
        })
        .catch(error => {
                if(error.response?.status === 404){
                    setEvents([])
                    setTotalPages(0)
                }
            })
    }

    useEffect(()=>{
        if(activeTab === "plans"){
            loadPlans()
        }else{
            loadEvents()
        }

    }, [activeTab, page, pageSize])


    return (

        <div>
            <select
                value={pageSize}
                onChange={(e) => {
                    setPageSize(Number(e.target.value));
                    setPage(0);
                }}
                >
                <option value={5}>5개씩 보기</option>
                <option value={10}>10개씩 보기</option>
                <option value={20}>20개씩 보기</option>
            </select>

            <button     title = "저장한 일정"
                        onClick = {()=>{
                            setActiveTab("plans")
                            setPage(0)
                        }}>
                저장한 일정
            </button>

            <button     title = "관심 행사"
                        onClick = {()=>{
                            setActiveTab("events")
                            setPage(0)
                        }}>
                관심 행사
            </button>

            {activeTab === "plans" && <PlansList ary={plans} onUpdate = {loadPlans}/>}
            {activeTab === "events" && <EventsList ary={events} onUpdate = {loadEvents}/>}

            {Array.from({ length: totalPages }, (_, index) => (
                <button     key={index}
                            onClick={() => setPage(index)}
                            disabled={page === index}>
                    {index + 1}
                </button>
            ))}

        </div>
    )
}

export default MyPage