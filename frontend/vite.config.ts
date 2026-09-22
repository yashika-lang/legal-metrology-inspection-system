import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";
import path from "node:path";

export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      "@": path.resolve(import.meta.dirname, "./src"),
    },
  },
  server: {
    port: 5173,
    // Cloudflare's free quick tunnels have died silently multiple times in
    // this same review (the local cloudflared process stays up while the
    // edge connection is dropped) — binding to every interface, not just
    // localhost, lets a phone on the same Wi-Fi reach this dev server
    // directly over the LAN (http://<this machine's LAN IP>:5173),
    // sidestepping that external dependency entirely when it's available.
    host: true,
    // Vite blocks unrecognized Host headers by default (DNS-rebinding
    // protection) — a Cloudflare quick tunnel forwards its own random
    // *.trycloudflare.com hostname. A quick tunnel has died silently and
    // needed a fresh restart with a brand-new random hostname three times
    // in this same review — a leading-dot entry matches every subdomain,
    // so any future tunnel keeps working without editing this file (and
    // restarting the dev server) each time.
    allowedHosts: [".trycloudflare.com"],
    // Proxies /api/* to the real backend so a browser reaching this dev
    // server through a tunnel (a different public origin) never needs its
    // own cross-origin call to localhost:8080 — one origin, no CORS
    // reconfiguration per tunnel URL.
    //
    // `changeOrigin` only rewrites the Host header, not Origin — a real
    // browser attaches its own Origin (e.g. the tunnel's https:// URL) to
    // this POST, and http-proxy forwards that Origin unchanged by default.
    // The backend's Spring Security CORS filter then sees a request that
    // looks like it came directly from an unrecognized cross-origin
    // browser and rejects it with 403 (CORS_ALLOWED_ORIGINS only lists
    // localhost) — even though, from the browser's own perspective, this
    // was a same-origin call to this dev server the whole time. Found
    // live: /auth/login and /auth/signup both 403'd through the tunnel
    // while curl (which never sends Origin) succeeded against the exact
    // same requests, masking the bug in every prior verification pass.
    // Rewriting Origin to this dev server's own origin before forwarding
    // makes the proxied request look like what it actually is — a
    // trusted, same-machine server-to-server call — so it keeps working
    // for any future tunnel hostname without touching backend config.
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
        configure: (proxy) => {
          proxy.on("proxyReq", (proxyReq) => {
            proxyReq.setHeader("origin", "http://localhost:5173");
          });
        },
      },
    },
  },
});
