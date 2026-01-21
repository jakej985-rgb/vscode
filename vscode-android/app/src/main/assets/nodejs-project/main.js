/**
 * CodePocket Local - Node.js Server
 */

const http = require('http');
const fs = require('fs');
const path = require('path');
const url = require('url');
const child_process = require('child_process');

// Configuration
const PORT = process.env.PORT || 13337;
const HOST = '127.0.0.1';
const PUBLIC_DIR = path.join(__dirname, 'public');
const LOG_FILE = process.env.LOG_PATH || path.join(__dirname, 'node.log');
const WORKSPACES_ROOT = process.env.WORKSPACES_ROOT || path.join(__dirname, '..', 'workspaces');

// Terminal Session Store
const terminals = {};

// MIME types
const MIME_TYPES = {
    '.html': 'text/html',
    '.css': 'text/css',
    '.js': 'application/javascript',
    '.json': 'application/json',
    '.png': 'image/png',
    '.jpg': 'image/jpeg',
    '.svg': 'image/svg+xml',
    '.md': 'text/markdown',
    '.txt': 'text/plain'
};

const startTime = Date.now();

// Utility Functions
function log(level, message) {
    const timestamp = new Date().toISOString();
    console.log(`[${timestamp}] [${level}] ${message}`);
    try { fs.appendFileSync(LOG_FILE, `[${timestamp}] [${level}] ${message}\n`); } catch (e) {}
}

function serveStaticFile(filePath, res) {
    fs.readFile(filePath, (err, data) => {
        if (err) {
            res.writeHead(err.code === 'ENOENT' ? 404 : 500, { 'Content-Type': 'text/plain' });
            res.end(err.code === 'ENOENT' ? '404 Not Found' : 'Internal Server Error');
            return;
        }
        const ext = path.extname(filePath).toLowerCase();
        res.writeHead(200, { 'Content-Type': MIME_TYPES[ext] || 'application/octet-stream', 'Access-Control-Allow-Origin': '*' });
        res.end(data);
    });
}

function createTerminal(cwd) {
    const id = Date.now().toString();
    const shell = '/system/bin/sh';
    const term = child_process.spawn(shell, [], {
        cwd: cwd && fs.existsSync(cwd) ? cwd : WORKSPACES_ROOT,
        env: process.env,
        stdio: ['pipe', 'pipe', 'pipe']
    });

    terminals[id] = { process: term, buffer: [] };

    function addToBuffer(data) {
        if (!terminals[id]) return;
        terminals[id].buffer.push(data.toString());
        if (terminals[id].buffer.length > 5000) terminals[id].buffer.shift();
    }

    term.stdout.on('data', addToBuffer);
    term.stderr.on('data', addToBuffer);
    term.on('close', () => delete terminals[id]);
    term.on('error', (err) => addToBuffer(`\r\nError: ${err.message}\r\n`));

    return id;
}

// Authentication
const AUTH_TOKEN_FILE = path.join(__dirname, '.auth_token');
let AUTH_TOKEN = null;
try {
    if (fs.existsSync(AUTH_TOKEN_FILE)) AUTH_TOKEN = fs.readFileSync(AUTH_TOKEN_FILE, 'utf8').trim();
} catch (e) { log('ERROR', 'Auth load failed: ' + e.message); }

// Server
const server = http.createServer((req, res) => {
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS, PUT, DELETE');

    if (req.method === 'OPTIONS') { res.writeHead(204); res.end(); return; }

    const parsedUrl = url.parse(req.url, true);
    const pathname = parsedUrl.pathname;
    log('INFO', `${req.method} ${pathname}`);

    if (pathname === '/health' || pathname === '/api/health') {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ status: 'ok', uptime: Math.floor((Date.now() - startTime) / 1000) }));
        return;
    }

    if (pathname.startsWith('/api/')) {
        handleApiRequest(req, res, pathname, parsedUrl.query);
        return;
    }

    // Static Files
    let filePath = path.join(PUBLIC_DIR, pathname === '/' ? 'index.html' : pathname);
    fs.stat(filePath, (err, stats) => {
        if (err || stats.isDirectory()) {
            const indexTry = path.join(filePath, stats && stats.isDirectory() ? 'index.html' : ''); // Try index if dir
            const htmlTry = filePath + '.html'; // Try extension
            
            if (stats && stats.isDirectory()) {
                 serveStaticFile(indexTry, res);
            } else {
                fs.stat(htmlTry, (e2) => {
                    if (!e2) serveStaticFile(htmlTry, res);
                    else {
                        res.writeHead(404);
                        res.end('404');
                    }
                });
            }
        } else {
            serveStaticFile(filePath, res);
        }
    });
});

function handleApiRequest(req, res, pathname, query) {
    // Auth Check
    if (AUTH_TOKEN) {
        let token = query.token;
        const auth = req.headers['authorization'];
        if (auth && auth.startsWith('Bearer ')) token = auth.substring(7);
        if (token !== AUTH_TOKEN) {
            res.writeHead(403, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({ error: 'Unauthorized' }));
            return;
        }
    }

    const apiPath = pathname.replace('/api/', '');

    if (apiPath === 'workspaces') {
        try {
            if (!fs.existsSync(WORKSPACES_ROOT)) fs.mkdirSync(WORKSPACES_ROOT, { recursive: true });
            const workspaces = fs.readdirSync(WORKSPACES_ROOT).filter(f => fs.statSync(path.join(WORKSPACES_ROOT, f)).isDirectory());
            res.writeHead(200, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({ workspaces }));
        } catch (e) { res.writeHead(500); res.end(JSON.stringify({error: e.message})); }
        return;
    }

    // Stat: GET /api/stat?ws=id&path=...
    if (apiPath === 'stat') {
        const fullPath = path.join(WORKSPACES_ROOT, query.ws, query.path || '');
        if (!fullPath.startsWith(path.join(WORKSPACES_ROOT, query.ws))) { res.writeHead(403); res.end(); return; }
        
        fs.stat(fullPath, (err, stats) => {
             if(err) { res.writeHead(404); res.end(JSON.stringify({error:err.message})); return; }
             res.writeHead(200, {'Content-Type': 'application/json'});
             res.end(JSON.stringify({
                 size: stats.size,
                 mtimeMs: stats.mtimeMs,
                 ctimeMs: stats.ctimeMs,
                 type: stats.isDirectory() ? 'directory' : 'file',
                 mode: stats.mode
             }));
        });
        return;
    }

    // Readdir: GET /api/readdir?ws=id&path=...
    if (apiPath === 'readdir') {
        const fullPath = path.join(WORKSPACES_ROOT, query.ws, query.path || '');
        if (!fullPath.startsWith(path.join(WORKSPACES_ROOT, query.ws))) { res.writeHead(403); res.end(); return; }

        fs.readdir(fullPath, (err, files) => {
             if(err) { res.writeHead(404); res.end(JSON.stringify({error:err.message})); return; }
             res.writeHead(200, {'Content-Type': 'application/json'});
             res.end(JSON.stringify({ files }));
        });
        return;
    }

    // File Operations: mkdir, unlink, rmdir
    if (['mkdir','unlink','rmdir'].includes(apiPath) && req.method==='POST') {
        const fullPath = path.join(WORKSPACES_ROOT, query.ws, query.path);
        if (!fullPath.startsWith(path.join(WORKSPACES_ROOT, query.ws))) { res.writeHead(403); res.end(); return; }
        
        const cb = (err) => {
            if(err && err.code !== 'ENOENT') { res.writeHead(500); res.end(JSON.stringify({error:err.message})); }
            else { res.writeHead(200); res.end('{}'); }
        };

        if (apiPath === 'mkdir') fs.mkdir(fullPath, {recursive:true}, cb);
        else if (apiPath === 'unlink') fs.unlink(fullPath, cb);
        else if (apiPath === 'rmdir') fs.rmdir(fullPath, cb);
        return;
    }

    if (apiPath === 'files') {
        const wsId = query.ws;
        if (!wsId || !fs.existsSync(path.join(WORKSPACES_ROOT, wsId))) {
            res.writeHead(404); res.end(JSON.stringify({error: 'Workspace not found'})); return;
        }
        res.writeHead(200, {'Content-Type': 'application/json'});
        res.end(JSON.stringify({ files: listFilesRecursive(path.join(WORKSPACES_ROOT, wsId)) }));
        return;
    }

    if (apiPath === 'file') {
        const fullPath = path.join(WORKSPACES_ROOT, query.ws, query.path);
        if (!fullPath.startsWith(path.join(WORKSPACES_ROOT, query.ws))) {
            res.writeHead(403); res.end(JSON.stringify({error: 'Access denied'})); return;
        }

        if (req.method === 'GET') {
            fs.readFile(fullPath, 'utf8', (err, data) => {
                if(err) { res.writeHead(404); res.end(JSON.stringify({error: err.message})); }
                else { res.writeHead(200, {'Content-Type': 'text/plain'}); res.end(data); }
            });
        } else if (req.method === 'POST') {
            let body = '';
            req.on('data', c => body += c);
            req.on('end', () => {
                try {
                    fs.mkdirSync(path.dirname(fullPath), {recursive: true});
                    fs.writeFileSync(fullPath, body);
                    res.writeHead(200); res.end(JSON.stringify({success: true}));
                } catch(e) { res.writeHead(500); res.end(JSON.stringify({error: e.message})); }
            });
        }
        return;
    }

    // Settings
    if (apiPath === 'settings') {
        const settingsPath = path.join(WORKSPACES_ROOT, '..', 'settings.json');
        if (req.method === 'GET') {
            if (fs.existsSync(settingsPath)) {
                res.writeHead(200, {'Content-Type': 'application/json'});
                fs.createReadStream(settingsPath).pipe(res);
            } else {
                res.writeHead(200, {'Content-Type': 'application/json'});
                res.end('{}');
            }
        } else if (req.method === 'POST') {
            let body = '';
            req.on('data', c => body += c);
            req.on('end', () => {
                 try { fs.writeFileSync(settingsPath, body); res.writeHead(200); res.end(JSON.stringify({success:true})); }
                 catch(e) { res.writeHead(500); res.end(JSON.stringify({error: e.message})); }
            });
        }
        return;
    }

    // Plugins
    if (apiPath === 'plugins') {
        const pluginsDir = path.join(WORKSPACES_ROOT, '..', 'plugins');
        if (!fs.existsSync(pluginsDir)) { try { fs.mkdirSync(pluginsDir, {recursive:true}); } catch(e){} res.end(JSON.stringify({plugins:[]})); return; }
        try {
            const files = fs.readdirSync(pluginsDir).filter(f => f.endsWith('.js'));
            res.writeHead(200, {'Content-Type': 'application/json'});
            res.end(JSON.stringify({ plugins: files }));
        } catch(e) { res.end(JSON.stringify({plugins:[]})); }
        return;
    }

    if (apiPath === 'plugin') {
        const pluginsDir = path.join(WORKSPACES_ROOT, '..', 'plugins');
        const p = query.name;
        if(!p || p.includes('/') || p.includes('\\')) { res.writeHead(403); res.end(); return; }
        try {
            const data = fs.readFileSync(path.join(pluginsDir, p), 'utf8');
            res.writeHead(200, {'Content-Type': 'application/javascript'});
            res.end(data);
        } catch(e) { res.writeHead(404); res.end(); }
        return;
    }

    // Search
    if (apiPath === 'search') {
        const wsId = query.ws;
        const q = query.q;
        if(!wsId || !q) { res.writeHead(400); res.end('[]'); return; }
        
        const results = [];
        const root = path.join(WORKSPACES_ROOT, wsId);
        
        function scan(dir) {
            try {
                const list = fs.readdirSync(dir);
                for(const f of list) {
                    if(f==='.git' || f==='node_modules' || f==='dist' || f==='build' || f==='.DS_Store') continue;
                    const fp = path.join(dir, f);
                    try {
                        const st = fs.statSync(fp);
                        if(st.isDirectory()) {
                            scan(fp);
                        } else if(st.isFile() && st.size < 500000) { // 500KB limit
                            const content = fs.readFileSync(fp, 'utf8');
                            const lines = content.split('\n');
                            for(let i=0; i<lines.length; i++) {
                                if(lines[i].includes(q)) {
                                    results.push({
                                        file: path.relative(root, fp).replace(/\\/g, '/'),
                                        line: i+1,
                                        text: lines[i].trim().substring(0, 80)
                                    });
                                    if(results.length >= 200) return;
                                }
                            }
                        }
                    } catch(e){}
                    if(results.length >= 200) return;
                }
            } catch(e){}
        }
        
        if(fs.existsSync(root)) scan(root);
        res.writeHead(200, {'Content-Type': 'application/json'});
        res.end(JSON.stringify(results));
        return;
    }

    // Terminals
    if (apiPath === 'terminals' && req.method === 'POST') {
        let body = '';
        req.on('data', c => body += c);
        req.on('end', () => {
            let cwd = WORKSPACES_ROOT;
            try { if(body) cwd = JSON.parse(body).cwd || cwd; } catch(e) {}
            const id = createTerminal(cwd);
            res.writeHead(200, {'Content-Type': 'application/json'});
            res.end(JSON.stringify({ id }));
        });
        return;
    }

    if (apiPath.startsWith('terminals/') && apiPath.endsWith('/input')) {
        const id = apiPath.split('/')[1];
        if (!terminals[id]) { res.writeHead(404); res.end(); return; }
        let body = '';
        req.on('data', c => body += c);
        req.on('end', () => {
             try { terminals[id].process.stdin.write(body); res.writeHead(200); res.end(); }
             catch(e) { res.writeHead(500); res.end(); }
        });
        return;
    }

    if (apiPath.startsWith('terminals/') && apiPath.endsWith('/output')) {
        const id = apiPath.split('/')[1];
        if (!terminals[id]) { res.writeHead(404); res.end(JSON.stringify({error:'Closed'})); return; }
        const output = terminals[id].buffer.splice(0, terminals[id].buffer.length).join('');
        res.writeHead(200, {'Content-Type': 'application/json'});
        res.end(JSON.stringify({ output }));
        return;
    }

    res.writeHead(404); res.end(JSON.stringify({error: 'Not found'}));
}

function listFilesRecursive(dir, base = dir) {
    let res = [];
    fs.readdirSync(dir).forEach(f => {
        const fp = path.join(dir, f);
        const rp = path.relative(base, fp).replace(/\\/g, '/');
        const st = fs.statSync(fp);
        if (st.isDirectory()) res.push({name: f, path: rp, type: 'directory', children: listFilesRecursive(fp, base)});
        else res.push({name: f, path: rp, type: 'file', size: st.size});
    });
    return res;
}

server.listen(PORT, HOST, () => log('INFO', `Server running at http://${HOST}:${PORT}/`));
