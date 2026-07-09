const axios = require('axios');
const cheerio = require('cheerio');

async function test() {
    try {
        console.log("Searching...");
        const res = await axios.get('https://4khdhub.one/?s=Dune+Part+Two');
        const $ = cheerio.load(res.data);
        const match = $('a.movie-card').filter((i, el) => {
            return $(el).find('.movie-card-title').text().toLowerCase().includes('dune');
        }).first();
        
        if (!match.length) {
            console.log("No match found!");
            return;
        }
        
        const url = match.attr('href');
        console.log("Found url:", url);
        
        const pageRes = await axios.get(url.startsWith('http') ? url : 'https://4khdhub.one' + url);
        const $page = cheerio.load(pageRes.data);
        
        const links = [];
        $page('.border.border-gray-800').each((i, block) => {
            const title = $(block).find('h3').text().trim();
            const downloadBtn = $(block).find('a[href*="hubcloud.club"], a[href*="gamerxyt.com"]').first();
            if (downloadBtn.length) {
                links.push({title, url: downloadBtn.attr('href')});
            }
        });
        
        console.log("Links found:", links);
        
    } catch (e) {
        console.error(e);
    }
}
test();
