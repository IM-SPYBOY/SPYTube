export async function onRequest(context) {
    const { request, env } = context;
    const db = env.DB;
    const ip = request.headers.get("CF-Connecting-IP") || "unknown";
    const ua = request.headers.get("User-Agent") || "unknown";

    try {
        await db.prepare("INSERT INTO visitors (ip, user_agent) VALUES (?, ?)")
            .bind(ip, ua)
            .run();
    } catch (e) {
        // Silent fail to avoid crashing the request
    }

    return new Response("OK", { status: 200 });
}
