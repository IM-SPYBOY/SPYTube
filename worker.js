/**
 * SPYTube Proxy Worker — Cloudflare Worker for ISP bypass (Layer 3)
 *
 * Deployed at: https://spytube-proxy.spytube.workers.dev
 *
 * Usage: GET https://spytube-proxy.spytube.workers.dev/https://api.hicine.info/api/trending
 *
 * The Worker fetches the real URL server-side (Cloudflare → origin).
 * From the ISP perspective: phone → Cloudflare IP (virtually never blocked).
 *
 * Deploy:
 *   wrangler deploy worker.js --name spytube-proxy
 *   OR paste this into Cloudflare Dashboard > Workers > Create Worker
 */

const ALLOWED_ORIGINS = [
  "cinefy.lol",
  "api.hicine.info",
  "hicine.info",
  "vidvault.ru",
];

const CORS_HEADERS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
  "Access-Control-Allow-Headers": "*",
};

export default {
  async fetch(request, env, ctx) {
    // Handle CORS preflight
    if (request.method === "OPTIONS") {
      return new Response(null, { headers: CORS_HEADERS });
    }

    const url = new URL(request.url);

    // The real target URL comes after the worker's origin in the pathname
    // e.g. /https://api.hicine.info/api/trending  →  https://api.hicine.info/api/trending
    const targetUrl = url.pathname.slice(1) + url.search;

    if (!targetUrl.startsWith("http")) {
      return new Response(
        JSON.stringify({ error: "Missing target URL. Usage: /https://target.domain/path" }),
        { status: 400, headers: { "Content-Type": "application/json", ...CORS_HEADERS } }
      );
    }

    let targetHost;
    try {
      targetHost = new URL(targetUrl).hostname;
    } catch {
      return new Response(
        JSON.stringify({ error: "Invalid target URL" }),
        { status: 400, headers: { "Content-Type": "application/json", ...CORS_HEADERS } }
      );
    }

    // Security: only proxy known SPYTube domains
    const allowed = ALLOWED_ORIGINS.some(
      (domain) => targetHost === domain || targetHost.endsWith("." + domain)
    );
    if (!allowed) {
      return new Response(
        JSON.stringify({ error: `Domain not allowed: ${targetHost}` }),
        { status: 403, headers: { "Content-Type": "application/json", ...CORS_HEADERS } }
      );
    }

    // Forward the original request headers (minus Host which CF sets automatically)
    const forwardHeaders = new Headers();
    for (const [key, value] of request.headers.entries()) {
      if (key.toLowerCase() !== "host" && !key.startsWith("cf-")) {
        forwardHeaders.set(key, value);
      }
    }
    // Set appropriate headers for each domain
    forwardHeaders.set("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36");
    if (targetHost.includes("cinefy.lol")) {
      forwardHeaders.set("Referer", "https://cinefy.lol/");
      forwardHeaders.set("Origin", "https://cinefy.lol");
    }
    if (targetHost.includes("vidvault.ru")) {
      forwardHeaders.set("Referer", "https://vidvault.ru/");
      forwardHeaders.set("Origin", "https://vidvault.ru");
    }

    try {
      const proxiedResp = await fetch(targetUrl, {
        method: request.method,
        headers: forwardHeaders,
        body: request.method !== "GET" && request.method !== "HEAD" ? request.body : undefined,
      });

      // Rebuild response with CORS headers
      const respHeaders = new Headers(proxiedResp.headers);
      Object.entries(CORS_HEADERS).forEach(([k, v]) => respHeaders.set(k, v));
      // Remove CORS restrictions from origin server
      respHeaders.delete("content-security-policy");
      respHeaders.delete("x-frame-options");

      return new Response(proxiedResp.body, {
        status: proxiedResp.status,
        statusText: proxiedResp.statusText,
        headers: respHeaders,
      });
    } catch (err) {
      return new Response(
        JSON.stringify({ error: `Proxy fetch failed: ${err.message}` }),
        { status: 502, headers: { "Content-Type": "application/json", ...CORS_HEADERS } }
      );
    }
  },
};
