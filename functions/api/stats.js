export async function onRequest(context) {
    const { env } = context;
    const db = env.DB;

    // Get total downloads (Sum of counts per tag)
    const downloadsResult = await db.prepare("SELECT SUM(count) as total FROM downloads").first();
    const downloads = downloadsResult ? downloadsResult.total : 0;

    // Get total visitors (Count Unique IPs)
    const visitorsResult = await db.prepare("SELECT COUNT(DISTINCT ip) as total FROM visitors").first();
    const visitors = visitorsResult ? visitorsResult.total : 0;

    // Get today's visitors (Count Unique IPs for today)
    const todayResult = await db.prepare("SELECT COUNT(DISTINCT ip) as count FROM visitors WHERE date(timestamp) = date('now')").first();
    const todayVisits = todayResult ? todayResult.count : 0;

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
