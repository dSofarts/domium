import type { NextConfig } from 'next'

const nextConfig: NextConfig = {
  reactCompiler: true,
  images: {
    remotePatterns: [
      {
        protocol: 'https',
        hostname: 'static.tildacdn.com',
        pathname: '/**'
      },
      {
        protocol: 'http',
        hostname: 'minio',
        port: '9000',
        pathname: '/**'
      }
    ]
  },
  output: 'standalone',
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
