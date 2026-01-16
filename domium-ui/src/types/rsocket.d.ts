declare module 'rsocket-core' {
  export class RSocketClient {
    constructor(options: unknown)
    connect(): {
      subscribe(observer: {
        onComplete?: (socket: any) => void
        onError?: (error: unknown) => void
      }): { cancel(): void }
    }
  }

  export const WellKnownMimeType: {
    MESSAGE_RSOCKET_ROUTING: { string: string }
  }

  export function encodeRoute(route: string): unknown
}

declare module 'rsocket-flowable' {
  export class Flowable<T = unknown> {
    subscribe(observer: {
      onComplete?: (payload: T) => void
      onError?: (error: unknown) => void
      onNext?: (payload: T) => void
    }): { cancel(): void }
  }
}

declare module 'rsocket-websocket-client' {
  const RSocketWebSocketClient: any
  export default RSocketWebSocketClient
}
