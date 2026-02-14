export async function onRequest(context) {
    const { request, env } = context;
    const db = env.DB;

    // Log visitor
    try {
        const stmt = db.prepare(`
      INSERT INTO visitors (date, count) 
      VALUES (DATE('now'), 1) 
      ON CONFLICT(date) DO UPDATE SET count = count + 1
    `);
        await stmt.run();
    } catch (e) {
        console.error("Failed to log visitor:", e);
    }

    return new Response("OK", { status: 200 });
}
