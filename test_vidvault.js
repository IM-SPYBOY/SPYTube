async function test() {
    try {
        const tokenRes = await fetch('https://vidvault.ru/api/get-token', {
            headers: {
                'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36',
                'Referer': 'https://vidvault.ru/',
                'Origin': 'https://vidvault.ru'
            }
        });
        const tokenData = await tokenRes.json();
        const token = tokenData.t;
        console.log("Token:", token);

        const dataRes = await fetch('https://vidvault.ru/api/download-proxy', {
            method: 'POST',
            body: JSON.stringify({
                type: "tv",
                tmdbId: "76479",
                season: 1,
                episode: 1
            }),
            headers: {
                'Content-Type': 'application/json',
                'x-request-token': token,
                'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36',
                'Referer': 'https://vidvault.ru/',
                'Origin': 'https://vidvault.ru'
            }
        });
        
        const text = await dataRes.text();
        console.log("Raw Response Length:", text.length);
        const data = JSON.parse(text);
        console.log("Response:", JSON.stringify(data).substring(0, 1000));
        console.log("extractData exists?", !!data.extractData);
        if (!data.extractData) {
            console.log("Keys available:", Object.keys(data));
            if (data.data) {
                console.log("data keys:", Object.keys(data.data));
            }
        }
    } catch(e) {
        console.log("Error:", e.message);
    }
}
test();
