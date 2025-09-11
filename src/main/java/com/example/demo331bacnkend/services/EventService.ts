/**
 * EventService - Axios client for Event APIs
 * - Read baseURL from Vite env: import.meta.env.VITE_BACKEND_URL
 * - Provide typed methods for list/get/create/update
 */

import axios, { AxiosInstance } from 'axios'

/** Event type matching your Spring Boot entity */
export interface Event {
    id?: number
    category?: string
    title: string
    description: string
    location: string
    date: string
    time?: string
    petAllowed?: boolean
    organizer?: string
}

/** Create axios client with env-based baseURL */
const apiClient: AxiosInstance = axios.create({
    // IMPORTANT: backend base URL from .env.development
    baseURL: import.meta.env.VITE_BACKEND_URL,
    withCredentials: false,
    headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json'
    },
    timeout: 15000
})

/** GET /events?_limit=&_page= */
export const fetchEvents = async (params?: {
    _limit?: number
    _page?: number
}) => {
    // Note: backend returns x-total-count in headers
    const res = await apiClient.get<Event[]>('/events', { params })
    return {
        data: res.data,
        total: Number(res.headers['x-total-count'] ?? res.data.length)
    }
}

/** GET /events/{id} */
export const fetchEventById = async (id: number) => {
    const res = await apiClient.get<Event>(`/events/${id}`)
    return res.data
}

/** POST /events */
export const createEvent = async (payload: Event) => {
    const res = await apiClient.post<Event>('/events', payload)
    return res.data
}

/** PUT /events  (if your backend exposes POST /events/save, change accordingly) */
export const updateEvent = async (payload: Event) => {
    const res = await apiClient.put<Event>('/events', payload)
    return res.data
}

/** helper to read current baseURL (for debugging UI) */
export const getBaseUrl = () => String(import.meta.env.VITE_BACKEND_URL)
