export async function onRequest(context) {
    const { env } = context;
    const db = env.DB;

    // Get total downloads
    const downloads = await db.prepare("SELECT SUM(count) as total FROM downloads").first("total");

    // Get total visitors
    const visitors = await db.prepare("SELECT SUM(count) as total FROM visitors").first("total");

    // Get today's visits
    const todayVisits = await db.prepare("SELECT count FROM visitors WHERE date = DATE('now')").first("count");

    return new Response(JSON.stringify({
        downloads: downloads || 0,
        visitors: visitors || 0,
        today: todayVisits || 0
    }), {
        headers: {
            "Content-Type": "application/json",
            "Access-Control-Allow-Origin": "*"
        }
    });
}
