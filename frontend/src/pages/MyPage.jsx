import React, { useState, useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { signup } from '../services/authService'
import '../styles/signup.css'
import axios from 'axios'

const MyPage = () => {

    //access token & email 
    const userid = localStorage.getItem('user')
    const at = localStorage.getItem('at')
    const rt = localStorage.getItem('rt')

    //페이지 훅
    const [activeTab, setActiveTab] = useState("plans");

    const [plans, setPlans] = useState([]);
    const [events, setEvents] = useState([]);

    const [page, setPage] = useState(0);
    const [pageSize, setPageSize] = useState(10);
    const [totalPages, setTotalPages] = useState(0);

    const loadPlans = async () =>{

        await axios.get(  `http://localhost:8080/api/plans`, 
                    {   headers : { Authorization : at ? at : ""},
                        params  : { user_id : userid,
                                    page : page,
                                    size : pageSize}
                    }
                )
            .then(response => {
                console.log(`debug >>>> MyPage loadPlans response : `, response)
                if(response.status === 200){
                    setPlans(response.data.items)

                    const totalPageCount = Math.ceil(response.data.totalCount / pageSize);
                    setTotalPages(totalPageCount);
                }
            })
            .catch(error => {
                if(error.response?.status === 404){
                    setPlans([])
                    setTotalPages(0)
                }
            }
            )
            .finally()
    }

    const loadEvents = async () => {

        await axios.get(  `http://localhost:8080/api/favorites/events`, 
                    {   headers : { Authorization : at ? at : ""},
                        params  : { user_id : userid,
                                    page : page,
                                    size : pageSize}
                    }
                )
            .then(response => {
                console.log(`debug >>>> MyPage loadEvents response : `, response)
                if(response.status === 200){
                    setEvents(response.data.items)

                    const totalPageCount = Math.ceil(response.data.totalCount / pageSize);
                    setTotalPages(totalPageCount);
                }
            })
            .catch(error => {
                if(error.response?.status === 404){
                    setEvents([])
                    setTotalPages(0)
                }
            })
            .finally()

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

            <Button     title = "저장한 일정"
                        onClick = {()=>{
                            setActiveTab("plans")
                            setPage(0)
                        }}>
                저장한 일정
            </Button>

            <Button     title = "관심 행사"
                        onClick = {()=>{
                            setActiveTab("events")
                            setPage(0)
                        }}>
                관심 행사
            </Button>

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