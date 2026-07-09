const axios = require('axios');
async function test() {
    try {
        const res = await axios.get('https://hicine.app/api/v1/search?q=Mortal+Kombat+2');
        console.log("Search for Mortal Kombat 2:", res.data);
    } catch(e) {
        console.log(e.message);
    }
}
test();
