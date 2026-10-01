/// <reference types="vite/client" />

declare module 'sockjs-client' {
  export default class SockJS {
    constructor(url: string)
  }
}

declare module '*.mjs' {
  export function toUtcIso(value: string): string
  export function toLocalDateTime(value: string): string
}
