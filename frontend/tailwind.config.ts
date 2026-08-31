import type { Config } from "tailwindcss";

const config: Config = {
  content: ["./src/**/*.{js,ts,jsx,tsx,mdx}"],
  theme: {
    extend: {
      colors: {
        brand: {
          50: "#EEF1FF",
          100: "#E0E4FF",
          400: "#7C7FF0",
          500: "#5B5FEF",
          600: "#4338CA",
          700: "#3730A3",
        },
        surface: "#F7F8FC",
        ink: {
          900: "#0F1222",
          700: "#3A3F55",
          500: "#6B7089",
          300: "#C3C7D9",
          100: "#EDEEF5",
        },
        good: "#16A34A",
        warn: "#D97706",
        bad: "#DC2626",
      },
      fontFamily: {
        sans: ["var(--font-sans)"],
        mono: ["var(--font-mono)"],
      },
      backgroundImage: {
        "card-gradient": "linear-gradient(135deg, #4338CA 0%, #5B5FEF 45%, #7C7FF0 100%)",
      },
      boxShadow: {
        card: "0 1px 2px rgba(15, 18, 34, 0.04), 0 8px 24px rgba(15, 18, 34, 0.06)",
      },
    },
  },
  plugins: [],
};
export default config;
