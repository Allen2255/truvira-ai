import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// Vite = the dev server + build tool. This file configures it.
export default defineConfig({
  plugins: [react()],        // lets Vite understand JSX (HTML-like syntax inside JavaScript)
  server: {
    port: 5173,              // MUST match @CrossOrigin(origins = "http://localhost:5173") in Spring Boot
  },
});
