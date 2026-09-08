import React from "react";
import { createRoot } from "react-dom/client";
import { AuthProvider } from "./app/AuthProvider";
import { AppRouter } from "./app/router";
import { I18nProvider } from "./i18n";
import "./styles/index.css";

createRoot(document.getElementById("root") as HTMLElement).render(
  <React.StrictMode>
    <I18nProvider>
      <AuthProvider>
        <AppRouter />
      </AuthProvider>
    </I18nProvider>
  </React.StrictMode>,
);
