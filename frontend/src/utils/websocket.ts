import SockJS from 'sockjs-client'
import { Client, IMessage, StompSubscription } from '@stomp/stompjs'
import { ElNotification } from 'element-plus'

export interface WebSocketMessage {
  type: 'DOCUMENT_PROGRESS' | 'AI_STREAM' | 'NOTIFICATION'
  payload: any
  timestamp: string
}

class WebSocketService {
  private client: Client | null = null
  private connected = false
  private subscriptions: Map<string, StompSubscription> = new Map()
  private messageHandlers: Map<string, (message: WebSocketMessage) => void> = new Map()
  private reconnectAttempts = 0
  private maxReconnectAttempts = 5
  private reconnectDelay = 3000

  connect(userId: string) {
    if (this.connected) {
      console.log('WebSocket already connected')
      return
    }

    const socket = new SockJS('http://localhost:8080/ws')

    this.client = new Client({
      webSocketFactory: () => socket as any,
      debug: (str) => {
        console.log('STOMP Debug:', str)
      },
      reconnectDelay: this.reconnectDelay,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,

      onConnect: () => {
        console.log('WebSocket connected')
        this.connected = true
        this.reconnectAttempts = 0

        // Subscribe to user-specific queue
        this.subscribeToUserQueue(userId)

        // Subscribe to broadcast topics if needed
        this.subscribeToBroadcast()

        // Show connection notification
        ElNotification({
          title: '连接成功',
          message: '实时推送已启用',
          type: 'success',
          duration: 2000
        })
      },

      onStompError: (frame) => {
        console.error('STOMP error:', frame)
        ElNotification({
          title: '连接错误',
          message: 'WebSocket 连接出错',
          type: 'error'
        })
      },

      onWebSocketClose: () => {
        console.log('WebSocket closed')
        this.connected = false

        if (this.reconnectAttempts < this.maxReconnectAttempts) {
          this.reconnectAttempts++
          console.log(`Reconnecting... (${this.reconnectAttempts}/${this.maxReconnectAttempts})`)
        } else {
          ElNotification({
            title: '连接断开',
            message: '无法连接到服务器',
            type: 'warning'
          })
        }
      }
    })

    this.client.activate()
  }

  private subscribeToUserQueue(userId: string) {
    if (!this.client) return

    const destination = `/queue/messages-${userId}`

    const subscription = this.client.subscribe(destination, (message: IMessage) => {
      this.handleMessage(message)
    })

    this.subscriptions.set('user-queue', subscription)
    console.log(`Subscribed to ${destination}`)
  }

  private subscribeToBroadcast() {
    if (!this.client) return

    // Subscribe to public topics if needed
    const subscription = this.client.subscribe('/topic/public', (message: IMessage) => {
      this.handleMessage(message)
    })

    this.subscriptions.set('broadcast', subscription)
  }

  private handleMessage(message: IMessage) {
    try {
      const wsMessage: WebSocketMessage = JSON.parse(message.body)
      console.log('Received WebSocket message:', wsMessage)

      // Call registered handlers
      const handler = this.messageHandlers.get(wsMessage.type)
      if (handler) {
        handler(wsMessage)
      }

      // Default handling for notifications
      if (wsMessage.type === 'NOTIFICATION') {
        this.showNotification(wsMessage.payload)
      }
    } catch (error) {
      console.error('Error handling WebSocket message:', error)
    }
  }

  private showNotification(payload: any) {
    const { title, message, level } = payload

    let type: any = 'info'
    if (level === 'success') type = 'success'
    else if (level === 'warning') type = 'warning'
    else if (level === 'error') type = 'error'

    ElNotification({
      title: title || '通知',
      message: message || '',
      type,
      duration: 4000
    })
  }

  // Register a handler for specific message type
  onMessage(type: string, handler: (message: WebSocketMessage) => void) {
    this.messageHandlers.set(type, handler)
  }

  // Remove handler
  offMessage(type: string) {
    this.messageHandlers.delete(type)
  }

  // Send message to server
  sendMessage(destination: string, body: any) {
    if (!this.client || !this.connected) {
      console.error('WebSocket not connected')
      return
    }

    this.client.publish({
      destination,
      body: JSON.stringify(body)
    })
  }

  disconnect() {
    if (!this.client) return

    // Unsubscribe all
    this.subscriptions.forEach(subscription => {
      subscription.unsubscribe()
    })
    this.subscriptions.clear()

    // Deactivate client
    this.client.deactivate()
    this.connected = false
    this.client = null

    console.log('WebSocket disconnected')
  }

  isConnected(): boolean {
    return this.connected
  }
}

// Singleton instance
export const websocketService = new WebSocketService()
