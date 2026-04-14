var http = require('http');

var HTML_ROUTES = {
    '/': '<html><body>ssr-root-page</body></html>',
    '/trainings': '<html><body>ssr-trainings-page</body></html>'
};

http.createServer(function (req, res) {
    var path = req.url.split('?')[0];
    if (HTML_ROUTES[path]) {
        res.writeHead(200, { 'Content-Type': 'text/html' });
        res.end(HTML_ROUTES[path]);
    } else if (path.startsWith('/_next/static/')) {
        res.writeHead(200, { 'Content-Type': 'application/javascript' });
        res.end('// static asset');
    } else {
        res.writeHead(404);
        res.end('Not found');
    }
}).listen(3000);