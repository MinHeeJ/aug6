/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  darkMode: "class",
  theme: {
    extend: {
      colors: {
        primary: "#5d87ff",
        secondary: "#49beff",
        success: "#13deb9",
        warning: "#f6b51e",
        error: "#ef4444",
        info: "#8754ec",
        dark: "#1c2536",
        link: "#2a3547",
        bodytext: "#5a6a85",
        muted: "#5a6a85",
        lightmuted: "#5d7287",
        lightgray: "#f6f9fc",
        lightprimary: "rgba(93, 135, 255, 0.12)",
        lightsecondary: "rgba(73, 190, 255, 0.12)",
        lightsuccess: "rgba(19, 222, 185, 0.12)",
        lightwarning: "rgba(246, 181, 30, 0.12)",
        lighterror: "rgba(239, 68, 68, 0.12)",
        lightinfo: "rgba(135, 84, 236, 0.12)",
        ld: "#e5e5e5",
        border: "#e5e5e5",
        "muted-foreground": "#737373",
      },
      borderRadius: {
        DEFAULT: "10px",
      },
      boxShadow: {
        md: "#919eab4d 0px 0px 2px 0px, #919eab1f 0px 12px 24px -4px",
        "btn-shadow": "#0000000d 0 9px 17.5px",
      },
    },
  },
  plugins: [],
};
