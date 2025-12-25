import type { NextConfig } from 'next'

const nextConfig: NextConfig = {
  reactCompiler: true,
  images: {
    domains: ['static.tildacdn.com']
  },
  async redirects() {
    return [
      {
        source: '/lk',
        destination: '/lk/projects',
        permanent: false
      }
    ]
  }
}

export default nextConfig
