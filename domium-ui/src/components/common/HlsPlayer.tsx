'use client'

import Hls from 'hls.js'
import { useEffect, useRef } from 'react'

interface HlsPlayerProps {
  src?: string | null
  poster?: string
  className?: string
  autoPlay?: boolean
  muted?: boolean
  controls?: boolean
  token?: string | null
}

export function HlsPlayer({
  src,
  poster,
  className,
  autoPlay = true,
  muted = true,
  controls = true,
  token
}: HlsPlayerProps) {
  const videoRef = useRef<HTMLVideoElement | null>(null)

  useEffect(() => {
    const video = videoRef.current
    if (!video || !src) return

    if (Hls.isSupported()) {
      const hls = new Hls({
        xhrSetup: xhr => {
          if (token) {
            xhr.setRequestHeader('Authorization', `Bearer ${token}`)
          }
        }
      })
      hls.loadSource(src)
      hls.attachMedia(video)
      return () => {
        hls.destroy()
      }
    }

    if (video.canPlayType('application/vnd.apple.mpegurl')) {
      video.src = src
    }
  }, [src])

  return (
    <video
      ref={videoRef}
      className={className}
      autoPlay={autoPlay}
      muted={muted}
      controls={controls}
      playsInline
      poster={poster}
    />
  )
}
