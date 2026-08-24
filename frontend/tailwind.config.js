/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        sentinel: {
          900: '#0b0f19',
          800: '#111827',
          700: '#1f2937',
          600: '#374151',
          accent: '#06b6d4', // Cyan
          danger: '#ef4444', // Red
          warning: '#f59e0b', // Amber
          success: '#10b981', // Emerald
        }
      }
    },
  },
  plugins: [],
}
