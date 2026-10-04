export type CoursePresentationBooking = { courseId: number; status: string }
export type CoursePresentationCourse = {
  id: number
  tenantId: number
  tenantName?: string
  startsAt: string
  endsAt: string
  capacity: number
  reservedCount?: number
  status?: string
}
export declare function hasActiveBooking(courseId: number, bookings?: CoursePresentationBooking[]): boolean
export declare function courseStatus(course: CoursePresentationCourse, bookings?: CoursePresentationBooking[], now?: Date, role?: string): { code: string; label: string }
export declare function groupCoursesByTenant<T extends { tenantId: number; tenantName?: string }>(courses?: T[]): Array<{ tenantId: number; name: string; courses: T[] }>
export declare function bookingStatusLabel(status: string): string
export declare function groupBookingsByStatus<T extends { status: string }>(bookings?: T[]): Array<{ status: string; label: string; count: number; bookings: T[] }>
