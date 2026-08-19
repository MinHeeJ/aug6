import React from "react";
import { createRoot } from "react-dom/client";
import { AuthProvider } from "./app/AuthProvider";
import { AppRouter } from "./app/router";
import "./styles/index.css";

createRoot(document.getElementById("root") as HTMLElement).render(
  <React.StrictMode>
    <AuthProvider>
      <AppRouter />
    </AuthProvider>
  </React.StrictMode>,
);
