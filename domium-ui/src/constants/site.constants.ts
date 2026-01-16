export const API_URL =
  process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8090'
export const BASE_URL =
  process.env.NEXT_PUBLIC_BASE_URL || 'http://localhost:3000'
export const KEYCLOAK_URL =
  process.env.NEXT_PUBLIC_KEYCLOAK_URL || 'http://keycloak.localhost:8080'
export const KEYCLOAK_REALM =
  process.env.NEXT_PUBLIC_KEYCLOAK_REALM || 'domium'
export const KEYCLOAK_CLIENT_ID =
  process.env.NEXT_PUBLIC_KEYCLOAK_CLIENT_ID || 'domium'
export const KEYCLOAK_CLIENT_SECRET =
  process.env.NEXT_PUBLIC_KEYCLOAK_CLIENT_SECRET ||
  'FaxzBgk7pkyattBrV8MlVCVg80jjZKo5'
export const CHAT_RSOCKET_URL =
  process.env.NEXT_PUBLIC_CHAT_RSOCKET_URL || 'ws://localhost:18085'
export const VIDEO_HLS_BASE_URL =
  process.env.NEXT_PUBLIC_VIDEO_HLS_BASE_URL || 'http://localhost:8088'
