
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        bg: {
          base: '#070b14',
          surface: '#0d1526',
          elevated: '#121d34',
          border: '#1a2740',
        },
        cyan: {
          400: '#22d3ee',
          500: '#06b6d4',
          DEFAULT: '#00d4ff',
        },
        violet: {
          400: '#a78bfa',
          500: '#8b5cf6',
          600: '#7c3aed',
        },
        risk: {
          low: '#10b981',
          medium: '#f59e0b',
          high: '#ef4444',
          critical: '#9333ea',
        },
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
        mono: ['JetBrains Mono', 'Fira Code', 'monospace'],
      },
      backgroundImage: {
        'gradient-cypher': 'linear-gradient(135deg, #00d4ff 0%, #7c3aed 100%)',
        'gradient-surface': 'linear-gradient(180deg, #0d1526 0%, #070b14 100%)',
        'gradient-card': 'linear-gradient(145deg, rgba(255,255,255,0.04) 0%, rgba(255,255,255,0.01) 100%)',
      },
      boxShadow: {
        glow: '0 0 20px rgba(0, 212, 255, 0.15)',
        'glow-violet': '0 0 20px rgba(124, 58, 237, 0.2)',
        card: '0 4px 24px rgba(0,0,0,0.4)',
        'card-hover': '0 8px 32px rgba(0,0,0,0.6)',
      },
      animation: {
        'fade-in': 'fadeIn 0.3s ease-out',
        'slide-up': 'slideUp 0.4s ease-out',
        'pulse-glow': 'pulseGlow 2s ease-in-out infinite',
        'spin-slow': 'spin 3s linear infinite',
      },
      keyframes: {
        fadeIn: { from: { opacity: 0 }, to: { opacity: 1 } },
        slideUp: { from: { opacity: 0, transform: 'translateY(12px)' }, to: { opacity: 1, transform: 'translateY(0)' } },
        pulseGlow: {
          '0%, 100%': { boxShadow: '0 0 10px rgba(0, 212, 255, 0.2)' },
          '50%': { boxShadow: '0 0 30px rgba(0, 212, 255, 0.5)' },
        },
      },
    },
  },
  plugins: [],
}
