export async function onRequest(context) {
    const { env } = context;
    const db = env.DB;

    // Fetch latest release from GitHub to get the tag and asset URL
    const response = await fetch("https://api.github.com/repos/IM-SPYBOY/spytube/releases/latest", {
        headers: { "User-Agent": "SPYTube-Landing-Page" }
    });

    if (!response.ok) {
        return new Response("Failed to fetch latest release", { status: 500 });
    }

    const data = await response.json();
    const tag = data.tag_name;
    const asset = data.assets.find(a => a.name.endsWith(".apk"));

    if (!asset) {
        return new Response("No APK found in release", { status: 404 });
    }

    // Increment download count in D1
    try {
        const stmt = db.prepare(`
      INSERT INTO downloads (release_tag, count) 
      VALUES (?, 1) 
      ON CONFLICT(release_tag) DO UPDATE SET count = count + 1
    `);
        await stmt.bind(tag).run();
    } catch (e) {
        console.error("Failed to log download:", e);
    }

    // Redirect to the actual GitHub asset URL
    return Response.redirect(asset.browser_download_url, 302);
}
