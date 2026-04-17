export async function onRequest(context) {
    const { env } = context;
    const db = env.DB;

    // Standard GitHub Shortcut URL (Always works, handles 302 itself)
    const DIRECT_DOWNLOAD_URL = "https://github.com/IM-SPYBOY/spytube/releases/latest/download/SPYTube.apk";

    // Async Stats Logging (Does not block the user download)
    context.waitUntil(
        (async () => {
            let tagName = "latest";
            try {
                // Fetch tag for stats only
                const response = await fetch("https://api.github.com/repos/IM-SPYBOY/spytube/releases/latest", {
                    headers: { "User-Agent": "SPYTube-Landing-Page" }
                });
                if (response.ok) {
                    const data = await response.json();
                    tagName = data.tag_name || "latest";
                }
            } catch (e) {
                // Keep default "latest"
            }

            // Log to D1
            try {
                await db.prepare(`
                    INSERT INTO downloads (release_tag, count) 
                    VALUES (?, 1) 
                    ON CONFLICT(release_tag) DO UPDATE SET count = count + 1
                `).bind(tagName).run();
            } catch (e) {
                console.error("Stats logging failed", e);
            }
        })()
    );

    // Immediate Redirect ensures Android compatibility
    return Response.redirect(DIRECT_DOWNLOAD_URL, 302);
}
