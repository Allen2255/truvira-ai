import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App.jsx";
import "./App.css";

// This is where React starts. It finds <div id="root"> in index.html
// and draws our <App /> component inside it.
// StrictMode = a development helper that warns about common mistakes.
ReactDOM.createRoot(document.getElementById("root")).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
