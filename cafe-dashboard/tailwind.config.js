/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,jsx}"],
  theme: {
    extend: {
      colors: {
        primary: "#4B2E1A",      // cafe brown
        "primary-light": "#7B4F2E",
        accent: "#D4A055",       // gold
        success: "#22C55E",
        warning: "#F59E0B",
        danger: "#EF4444",
        surface: "#FFFFFF",
        background: "#F5F0EB",   // warm off-white
      }
    },
  },
  plugins: [],
}

