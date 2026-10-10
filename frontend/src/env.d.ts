/// <reference types="vite/client" />

declare module 'sockjs-client' {
  export default class SockJS {
    constructor(url: string)
  }
}

declare module '*.mjs' {
  export function toUtcIso(value: string): string
  export function toLocalDateTime(value: string): string
  export function hasActiveBooking(courseId: number, bookings?: Array<{ courseId: number; status: string }>): boolean
  export function courseStatus(course: { id: number; startsAt: string; endsAt: string; capacity: number; reservedCount?: number; status?: string }, bookings?: Array<{ courseId: number; status: string }>, now?: Date, role?: string): { code: string; label: string }
  export function groupCoursesByTenant<T extends { tenantId: number; tenantName?: string }>(courses?: T[]): Array<{ tenantId: number; name: string; courses: T[] }>
  export function bookingStatusLabel(status: string): string
  export function groupBookingsByStatus<T extends { status: string }>(bookings?: T[]): Array<{ status: string; label: string; count: number; bookings: T[] }>
}
